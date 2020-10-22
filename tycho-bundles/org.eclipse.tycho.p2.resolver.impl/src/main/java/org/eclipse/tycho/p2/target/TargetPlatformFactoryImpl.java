/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - split target platform computation and dependency resolution
 *    SAP SE - create immutable target platform instances
 *    Christoph Läubrich    - [Bug 538144] Support other target locations (Directory, Features, Installations)
 *                          - [Bug 533747] Target file is read and parsed over and over again
 *                          - [Bug 567098] pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.local.MirroringArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.LazyArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.ProviderOnlyArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.ArtifactRepositoryBlackboard;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;

public class TargetPlatformFactoryImpl implements TargetPlatformFactory {

    private final MavenContext mavenContext;
    private final MavenLogger logger;
    private final IProgressMonitor monitor;

    private final IProvisioningAgent remoteAgent;
    private final IRepositoryIdManager remoteRepositoryIdManager;
    private final IMetadataRepositoryManager remoteMetadataRepositoryManager;
    private final IArtifactRepositoryManager remoteArtifactRepositoryManager;
    private final boolean offline;

    /** The Maven local repository as p2 IArtifactRepository */
    private final LocalArtifactRepository localArtifactRepository;

    /** The Maven local repository as p2 IMetadataRepository */
    private final LocalMetadataRepository localMetadataRepository;

    private final TargetDefinitionResolverService targetDefinitionResolverService;

    public TargetPlatformFactoryImpl(MavenContext mavenContext, IProvisioningAgent remoteAgent,
            LocalArtifactRepository localArtifactRepo, LocalMetadataRepository localMetadataRepo,
            TargetDefinitionResolverService targetDefinitionResolverService) {
        this.mavenContext = mavenContext;
        this.logger = mavenContext.getLogger();
        this.monitor = new DuplicateFilteringLoggingProgressMonitor(logger); // entails that this class is not thread-safe

        this.remoteAgent = remoteAgent;
        this.remoteRepositoryIdManager = (IRepositoryIdManager) remoteAgent
                .getService(IRepositoryIdManager.SERVICE_NAME);
        this.offline = mavenContext.isOffline();

        this.remoteMetadataRepositoryManager = (IMetadataRepositoryManager) remoteAgent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        if (remoteMetadataRepositoryManager == null) {
            throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
        }

        this.remoteArtifactRepositoryManager = (IArtifactRepositoryManager) remoteAgent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        if (remoteArtifactRepositoryManager == null) {
            throw new IllegalStateException("No artifact repository manager found"); //$NON-NLS-1$
        }

        this.localMetadataRepository = localMetadataRepo;
        this.localArtifactRepository = localArtifactRepo;
        this.targetDefinitionResolverService = targetDefinitionResolverService;
    }

    @Override
    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects,
            PomDependencyCollector pomDependencies) {
        return createTargetPlatform(tpConfiguration, ExecutionEnvironmentResolutionHandler.adapt(eeConfiguration),
                reactorProjects, pomDependencies);
    }

    /**
     * Computes the target platform from the given configuration and content.
     * 
     * <p>
     * Used as entry point for tests, which can provide the execution environment configuration via
     * the more low-level type {@link ExecutionEnvironmentResolutionHandler}.
     * </p>
     * 
     * @param tpConfiguration
     * @param eeResolutionHandler
     *            Representation of the target execution environment profile. In case of a custom EE
     *            profile, the handler also reads the full specification from the target platform.
     * @param reactorProjects
     *            may be <code>null</code>
     * @param pomDependencies
     *            may be <code>null</code>
     * 
     * @see #createTargetPlatform(TargetPlatformConfigurationStub,
     *      ExecutionEnvironmentConfiguration, List, PomDependencyCollector)
     */
    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentResolutionHandler eeResolutionHandler, List<ReactorProject> reactorProjects,
            PomDependencyCollector pomDependencies) {
        if (pomDependencies == null) {
            pomDependencies = new PomDependencyCollectorImpl(mavenContext, null);
        }
        List<TargetDefinitionContent> targetFileContent = resolveTargetDefinitions(tpConfiguration,
                eeResolutionHandler.getResolutionHints());

        PomDependencyCollectorImpl pomDependenciesContent = (PomDependencyCollectorImpl) pomDependencies;

        // TODO 372780 get rid of this special handling of pomDependency artifacts: there should be one p2 artifact repo view on the target platform
        IRawArtifactFileProvider pomDependencyArtifactRepo = pomDependenciesContent.getArtifactRepoOfPublishedBundles();
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey
                .forResolutionContextArtifacts(pomDependenciesContent.getProjectLocation());
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, new ProviderOnlyArtifactRepository(
                pomDependencyArtifactRepo, Activator.getProvisioningAgent(), blackboardKey.toURI()));
        logger.debug("Registered artifact repository " + blackboardKey);

        Set<MavenRepositoryLocation> completeRepositories = tpConfiguration.getP2Repositories();
        registerRepositoryIDs(completeRepositories);

        // collect & process metadata
        boolean includeLocalMavenRepo = shouldIncludeLocallyInstalledUnits(tpConfiguration);
        LinkedHashSet<IInstallableUnit> externalUIs = gatherExternalInstallableUnits(completeRepositories,
                targetFileContent, pomDependenciesContent, includeLocalMavenRepo);

        Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectUIs = getPreliminaryReactorProjectUIs(
                reactorProjects);

        List<TargetPlatformFilter> iuFilters = tpConfiguration.getFilters();
        TargetPlatformFilterEvaluator filter = !iuFilters.isEmpty()
                ? new TargetPlatformFilterEvaluator(iuFilters, logger)
                : null;

        applyConfiguredFilter(filter, reactorProjectUIs.keySet());
        applyFilters(filter, externalUIs, reactorProjectUIs.keySet(), eeResolutionHandler.getResolutionHints());

        PreliminaryTargetPlatformImpl targetPlatform = new PreliminaryTargetPlatformImpl(reactorProjectUIs, //
                externalUIs, //
                pomDependenciesContent.getMavenInstallableUnits(), //
                eeResolutionHandler.getResolutionHints(), //
                filter, //
                localMetadataRepository, //
                createExternalArtifactProvider(completeRepositories, targetFileContent, pomDependencyArtifactRepo,
                        tpConfiguration.getIncludePackedArtifacts()), //
                localArtifactRepository, //
                includeLocalMavenRepo, //
                logger);

        eeResolutionHandler.readFullSpecification(targetPlatform.getInstallableUnits());

        return targetPlatform;
    }

    private List<TargetDefinitionContent> resolveTargetDefinitions(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentResolutionHints eeResolutionHints) {
        List<TargetDefinitionContent> result = new ArrayList<>();

        for (TargetDefinition definition : tpConfiguration.getTargetDefinitions()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding target definition file \"" + definition.getOrigin() + "\"");
            }

            TargetDefinitionContent targetFileContent = targetDefinitionResolverService.getTargetDefinitionContent(
                    definition, tpConfiguration.getEnvironments(), eeResolutionHints,
                    tpConfiguration.getIncludeSourceMode(), remoteAgent);
            result.add(targetFileContent);

            if (logger.isDebugEnabled()) {
                logger.debug("Target definition file contains " + targetFileContent
                        .query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet().size() + " units");
            }
        }
        return result;
    }

    /**
     * Register the IDs of repositories which have an explicit ID. The IDs are used to pick mirrors
     * and to configure credentials when loading the repositories.
     */
    private void registerRepositoryIDs(Set<MavenRepositoryLocation> repositoriesWithIDs) {
        for (MavenRepositoryLocation location : repositoriesWithIDs) {
            remoteRepositoryIdManager.addMapping(location.getId(), location.getURL());
        }
    }

    private boolean shouldIncludeLocallyInstalledUnits(TargetPlatformConfigurationStub tpConfiguration) {
        if (tpConfiguration.getForceIgnoreLocalArtifacts()) {
            return false;

        } else {
            // check if disabled on command line or via Maven settings
            boolean ignoreLocal = "ignore"
                    .equalsIgnoreCase(mavenContext.getSessionProperties().getProperty("tycho.localArtifacts"));
            if (ignoreLocal) {
                logger.debug("tycho.localArtifacts="
                        + mavenContext.getSessionProperties().getProperty("tycho.localArtifacts")
                        + " -> ignoring locally built artifacts");
                return false;
            }
        }
        // default: include locally installed artifacts in target platform
        return true;
    }

    /**
     * External installable units collected from p2 repositories, .target files and local Maven
     * repository.
     */
    private LinkedHashSet<IInstallableUnit> gatherExternalInstallableUnits(
            Set<MavenRepositoryLocation> completeRepositories, List<TargetDefinitionContent> targetDefinitionsContent,
            PomDependencyCollectorImpl pomDependenciesContent, boolean includeLocalMavenRepo) {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<>();

        for (TargetDefinitionContent targetDefinitionContent : targetDefinitionsContent) {
            IQueryResult<IInstallableUnit> queryResult = targetDefinitionContent.query(QueryUtil.ALL_UNITS, monitor);
            result.addAll(queryResult.toUnmodifiableSet());
        }

        List<IMetadataRepository> metadataRepositories = new ArrayList<>();
        for (MavenRepositoryLocation location : completeRepositories) {
            metadataRepositories.add(loadMetadataRepository(location));
        }
        if (includeLocalMavenRepo) {
            metadataRepositories.add(localMetadataRepository);
        }

        for (IMetadataRepository repository : metadataRepositories) {
            IQueryResult<IInstallableUnit> matches = repository.query(QueryUtil.ALL_UNITS, monitor);
            result.addAll(matches.toUnmodifiableSet());
        }

        result.addAll(pomDependenciesContent.gatherMavenInstallableUnits());

        if (includeLocalMavenRepo && logger.isDebugEnabled()) {
            IQueryResult<IInstallableUnit> locallyInstalledIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS,
                    null);
            logger.debug("Added " + countElements(locallyInstalledIUs.iterator())
                    + " locally built units to the target platform");
        }

        return result;
    }

    private IMetadataRepository loadMetadataRepository(MavenRepositoryLocation location) {
        try {
            // TODO always log that a p2 repository is added to the target platform somewhere; used to be either from p2 or the following line
            // logger.info("Adding repository (cached) " + location.toASCIIString());

            return remoteMetadataRepositoryManager.loadRepository(location.getURL(), monitor);

        } catch (ProvisionException e) {
            String idMessage = location.getId() == null ? "" : " with ID '" + location.getId() + "'";
            throw new RuntimeException(
                    "Failed to load p2 repository" + idMessage + " from location " + location.getURL(), e);
        }
    }

    /**
     * Provider for all target platform artifacts from outside the reactor.
     */
    private IRawArtifactFileProvider createExternalArtifactProvider(Set<MavenRepositoryLocation> completeRepositories,
            List<TargetDefinitionContent> targetDefinitionsContent,
            IRawArtifactFileProvider pomDependencyArtifactRepository, boolean includePackedArtifacts) {

        RepositoryArtifactProvider remoteArtifactProvider = createRemoteArtifactProvider(completeRepositories,
                targetDefinitionsContent);
        MirroringArtifactProvider remoteArtifactCache = MirroringArtifactProvider
                .createInstance(localArtifactRepository, remoteArtifactProvider, includePackedArtifacts, logger);

        IRawArtifactFileProvider jointArtifactsProvider = new CompositeArtifactProvider(pomDependencyArtifactRepository,
                remoteArtifactCache);
        return jointArtifactsProvider;
    }

    /**
     * Provider for the target platform artifacts not yet available in the local Maven repository.
     */
    private RepositoryArtifactProvider createRemoteArtifactProvider(Set<MavenRepositoryLocation> mavenRepositories,
            List<TargetDefinitionContent> targetDefinitionsContent) {
        List<IArtifactRepository> artifactRepositories = new ArrayList<>();

        for (MavenRepositoryLocation location : mavenRepositories) {
            if (!offline || URIUtil.isFileURI(location.getURL())) {
                artifactRepositories.add(new LazyArtifactRepository(remoteAgent, location.getURL(),
                        RepositoryArtifactProvider::loadRepository));
            }
        }

        for (TargetDefinitionContent content : targetDefinitionsContent) {
            artifactRepositories.add(content.getArtifactRepository());
        }
        return new RepositoryArtifactProvider(artifactRepositories, ArtifactTransferPolicies.forRemoteArtifacts());
    }

    private Map<IInstallableUnit, ReactorProjectIdentities> getPreliminaryReactorProjectUIs(
            List<ReactorProject> reactorProjects) throws DuplicateReactorIUsException {
        if (reactorProjects == null) {
            return Collections.emptyMap();
        }

        Map<IInstallableUnit, ReactorProjectIdentities> reactorUIs = new HashMap<>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<>();

        for (ReactorProject project : reactorProjects) {
            @SuppressWarnings("unchecked")
            Set<IInstallableUnit> projectIUs = (Set<IInstallableUnit>) project.getDependencyMetadata();
            if (projectIUs == null)
                continue;

            for (IInstallableUnit iu : projectIUs) {
                ReactorProjectIdentities otherOrigin = reactorUIs.put(iu, project.getIdentities());

                if (otherOrigin != null && !otherOrigin.equals(project.getIdentities())) {
                    Set<File> duplicateLocations = duplicateReactorUIs.get(iu);
                    if (duplicateLocations == null) {
                        duplicateLocations = new LinkedHashSet<>();
                        duplicateReactorUIs.put(iu, duplicateLocations);
                    }
                    duplicateLocations.add(otherOrigin.getBasedir());
                    duplicateLocations.add(project.getBasedir());
                }
            }
        }

        if (!duplicateReactorUIs.isEmpty()) {
            // TODO 392320 we should only fail if IUs with same id and version but different content are found
            throw new DuplicateReactorIUsException(duplicateReactorUIs);
        }

        return reactorUIs;
    }

    private void applyFilters(TargetPlatformFilterEvaluator filter, Collection<IInstallableUnit> collectionToModify,
            Set<IInstallableUnit> reactorProjectUIs, ExecutionEnvironmentResolutionHints eeResolutionHints) {

        Set<String> reactorIUIDs = new HashSet<>();
        for (IInstallableUnit unit : reactorProjectUIs) {
            reactorIUIDs.add(unit.getId());
        }

        // a.jre/config.a.jre installable units
        // partial installable units
        // installable units shadowed by reactor projects
        Iterator<IInstallableUnit> iter = collectionToModify.iterator();
        while (iter.hasNext()) {
            IInstallableUnit unit = iter.next();
            if (eeResolutionHints.isNonApplicableEEUnit(unit) || isPartialIU(unit)
                    || reactorIUIDs.contains(unit.getId())) {
                // TODO debug log output?
                iter.remove();
                continue;
            }
        }

        applyConfiguredFilter(filter, collectionToModify);
    }

    private static void applyConfiguredFilter(TargetPlatformFilterEvaluator filter,
            Collection<IInstallableUnit> collectionToModify) {
        if (filter != null) {
            filter.filterUnits(collectionToModify);
        }
    }

    private static boolean isPartialIU(IInstallableUnit iu) {
        return Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue();
    }

    static int countElements(Iterator<?> iterator) {
        int result = 0;
        for (; iterator.hasNext(); iterator.next()) {
            ++result;
        }
        return result;
    }

    public P2TargetPlatform createTargetPlatformWithUpdatedReactorContent(TargetPlatform baseTargetPlatform,
            List<PublishingRepository> upstreamProjectResults) {
        return createTargetPlatformWithUpdatedReactorUnits(baseTargetPlatform,
                extractProjectResultIUs(upstreamProjectResults), getProjectArtifactProviders(upstreamProjectResults));
    }

    P2TargetPlatform createTargetPlatformWithUpdatedReactorUnits(TargetPlatform baseTargetPlatform,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorUnits,
            List<IRawArtifactFileProvider> reactorArtifacts) {
        if (!(baseTargetPlatform instanceof PreliminaryTargetPlatformImpl)) {
            throw new IllegalArgumentException(
                    "Base target platform must be an instance of PreliminaryTargetPlatformImpl; was: "
                            + baseTargetPlatform);
        }
        return createTargetPlatformWithUpdatedReactorUnits((PreliminaryTargetPlatformImpl) baseTargetPlatform,
                reactorUnits, reactorArtifacts);
    }

    P2TargetPlatform createTargetPlatformWithUpdatedReactorUnits(PreliminaryTargetPlatformImpl preliminaryTP,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorUnitsMap,
            List<IRawArtifactFileProvider> reactorArtifacts) {

        LinkedHashSet<IInstallableUnit> allUnits = preliminaryTP.getExternalUnits();

        // external units are already filtered, only reactor units need to be filtered again
        if (reactorUnitsMap != null) {
            Collection<IInstallableUnit> reactorUnits = new LinkedList<>(reactorUnitsMap.keySet());

            TargetPlatformFilterEvaluator configuredFilters = preliminaryTP.getFilter();
            if (configuredFilters != null) {
                configuredFilters.filterUnits(reactorUnits);
            }
            allUnits.addAll(reactorUnits);
        }

        IRawArtifactFileProvider jointArtifacts = createJointArtifactProvider(reactorArtifacts,
                preliminaryTP.getExternalArtifacts());

        return new FinalTargetPlatformImpl(allUnits, preliminaryTP.getEEResolutionHints(), jointArtifacts,
                localArtifactRepository, preliminaryTP.getOriginalMavenArtifactMap(), reactorUnitsMap);
    }

    private CompositeArtifactProvider createJointArtifactProvider(List<IRawArtifactFileProvider> reactorArtifacts,
            IRawArtifactFileProvider externalArtifacts) {
        // prefer artifacts from the reactor
        return new CompositeArtifactProvider(reactorArtifacts, Collections.singletonList(externalArtifacts));
    }

    private static Map<IInstallableUnit, ReactorProjectIdentities> extractProjectResultIUs(
            List<PublishingRepository> projectResults) {
        Map<IInstallableUnit, ReactorProjectIdentities> reactorUnits = new LinkedHashMap<>();
        for (PublishingRepository projectResult : projectResults) {
            Set<IInstallableUnit> projectUnits = projectResult.getMetadataRepository().query(QueryUtil.ALL_UNITS, null)
                    .toUnmodifiableSet();
            ReactorProjectIdentities project = projectResult.getProjectIdentities();
            for (IInstallableUnit projectUnit : projectUnits) {
                reactorUnits.put(projectUnit, project);
            }
        }
        return reactorUnits;
    }

    private static List<IRawArtifactFileProvider> getProjectArtifactProviders(
            List<PublishingRepository> upstreamProjectResults) {
        List<IRawArtifactFileProvider> artifactProviders = new ArrayList<>();
        for (PublishingRepository upstreamProject : upstreamProjectResults) {
            artifactProviders.add(upstreamProject.getArtifacts());
        }
        return artifactProviders;
    }
}
