/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.impl.resolver.LoggingProgressMonitor;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.local.MirroringArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicies;
import org.eclipse.tycho.repository.p2base.artifact.repository.ProviderOnlyArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.repository.registry.ArtifactRepositoryBlackboard;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;

// TODO 412416 refactor; current version minimizes the diff to the former TargetPlatformBuildImpl class
public class TargetPlatformFactoryImpl implements TargetPlatformFactory {

    private final MavenLogger logger;

    private final IProgressMonitor monitor;

    private final boolean offline;

    private final IProvisioningAgent remoteAgent;

    private final IMetadataRepositoryManager remoteMetadataRepositoryManager;
    private final IArtifactRepositoryManager remoteArtifactRepositoryManager;
    private IRepositoryIdManager remoteRepositoryIdManager;

    /** maven local repository as P2 IArtifactRepository */
    private final LocalArtifactRepository localArtifactRepository;

    /** maven local repository as P2 IMetadataRepository */
    private final LocalMetadataRepository localMetadataRepository;

    private final TargetDefinitionResolverService targetDefinitionResolverService;

    private MavenContext mavenContext;

    public TargetPlatformFactoryImpl(MavenContext mavenContext, IProvisioningAgent remoteAgent,
            LocalArtifactRepository localArtifactRepo, LocalMetadataRepository localMetadataRepo,
            TargetDefinitionResolverService targetDefinitionResolverService) {
        this.mavenContext = mavenContext;

        this.remoteAgent = remoteAgent;
        this.targetDefinitionResolverService = targetDefinitionResolverService;
        this.logger = mavenContext.getLogger();
        this.monitor = new LoggingProgressMonitor(logger);

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

        this.remoteRepositoryIdManager = (IRepositoryIdManager) remoteAgent
                .getService(IRepositoryIdManager.SERVICE_NAME);

        this.offline = mavenContext.isOffline();

        this.localMetadataRepository = localMetadataRepo;
        this.localArtifactRepository = localArtifactRepo;

    }

    private void addP2Repository(MavenRepositoryLocation location, List<IMetadataRepository> metadataRepositories) {
        IMetadataRepository metadataRepository = null;

        try {
            remoteRepositoryIdManager.addMapping(location.getId(), location.getURL());

            // TODO always log that a p2 repository is added to the target platform somewhere; used to be either from p2 or the following line
            // logger.info("Adding repository (cached) " + location.toASCIIString());

            metadataRepository = remoteMetadataRepositoryManager.loadRepository(location.getURL(), monitor);
            metadataRepositories.add(metadataRepository);

            // processPartialIUs( metadataRepository, artifactRepository );
        } catch (ProvisionException e) {
            String idMessage = location.getId() == null ? "" : " with ID '" + location.getId() + "'";
            throw new RuntimeException("Failed to load p2 repository" + idMessage + " from location "
                    + location.getURL(), e);
        }
    }

    private List<TargetDefinitionContent> resolveTargetDefinitions(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentResolutionHandler eeResolutionHandler) {
        List<TargetDefinitionContent> result = new ArrayList<TargetDefinitionContent>();

        for (TargetDefinition definition : tpConfiguration.getTargetDefinitions()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding target definition file \"" + definition.getOrigin() + "\"");
            }

            List<TargetEnvironment> environments = tpConfiguration.getEnvironments();
            TargetDefinitionContent targetFileContent = targetDefinitionResolverService.getTargetDefinitionContent(
                    definition, environments, eeResolutionHandler.getResolutionHints(), remoteAgent);
            result.add(targetFileContent);

            if (logger.isDebugEnabled()) {
                logger.debug("Added " + targetFileContent.getUnits().size()
                        + " units, the content of the target definition file, to the target platform");
            }
        }
        return result;
    }

    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<IReactorArtifactFacade> reactorArtifacts,
            PomDependencyCollector pomDependencies) {
        return createTargetPlatform(tpConfiguration, ExecutionEnvironmentResolutionHandler.adapt(eeConfiguration),
                reactorArtifacts, pomDependencies);
    }

    // TODO complete javadoc; move to interface
    /**
     * @param eeResolutionHandler
     *            Representation of the target execution environment profile. In case of a custom EE
     *            profile, the handler also reads the full specification from the target platform.
     * @param reactorProjects
     *            may be <code>null</code>
     * @param pomDependencies
     *            may be <code>null</code>
     */
    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentResolutionHandler eeResolutionHandler, List<IReactorArtifactFacade> reactorProjects,
            PomDependencyCollector pomDependencies) {
        List<TargetDefinitionContent> targetFileContent = resolveTargetDefinitions(tpConfiguration, eeResolutionHandler);

        PomDependencyCollectorImpl pomDependenciesContent = (PomDependencyCollectorImpl) pomDependencies;
        // TODO 412416 remove when the RepositoryBlackboardKey registration is gone
        if (pomDependenciesContent == null)
            pomDependenciesContent = new PomDependencyCollectorImpl(mavenContext);

        // TODO 372780 get rid of this special handling of pomDependency artifacts: there should be one p2 artifact repo view on the target platform
        IRawArtifactFileProvider pomDependencyArtifactRepo = pomDependenciesContent.getArtifactRepoOfPublishedBundles();
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey
                .forResolutionContextArtifacts(pomDependenciesContent.getProjectLocation());
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, new ProviderOnlyArtifactRepository(
                pomDependencyArtifactRepo, Activator.getProvisioningAgent(), blackboardKey.toURI()));
        logger.debug("Registered artifact repository " + blackboardKey);

        if (reactorProjects == null) {
            reactorProjects = Collections.emptyList();
        }
        Set<IInstallableUnit> reactorProjectUIs = getReactorProjectUIs(reactorProjects,
                tpConfiguration.getFailOnDuplicateIUs());

        List<TargetPlatformFilter> iuFilters = tpConfiguration.getFilters();
        TargetPlatformFilterEvaluator filter = !iuFilters.isEmpty() ? new TargetPlatformFilterEvaluator(iuFilters,
                logger) : null;

        boolean includeLocalMavenRepo = tpConfiguration.getForceIgnoreLocalArtifacts() ? false
                : shouldIncludeLocalMavenRepo(mavenContext);
        LinkedHashSet<IInstallableUnit> externalUIs = gatherExternalInstallableUnits(targetFileContent,
                tpConfiguration, includeLocalMavenRepo);
        applyFilters(filter, externalUIs, reactorProjectUIs, eeResolutionHandler.getResolutionHints());

        // TODO 396999 mavenIUs is never read after filtering
        LinkedHashSet<IInstallableUnit> mavenIUs = pomDependenciesContent.gatherMavenInstallableUnits();
        applyFilters(filter, mavenIUs, reactorProjectUIs, eeResolutionHandler.getResolutionHints());

        List<URI> allRemoteArtifactRepositories = new ArrayList<URI>();
        for (MavenRepositoryLocation location : tpConfiguration.getP2Repositories()) {
            if (!offline || URIUtil.isFileURI(location.getURL())) {
                // TODO 412416 id registration already happened here; make this more obvious
                allRemoteArtifactRepositories.add(location.getURL());
            }
        }
        for (TargetDefinitionContent contentPart : targetFileContent) {
            allRemoteArtifactRepositories.addAll(contentPart.getArtifactRepositoryLocations());
        }

        RepositoryArtifactProvider remoteArtifacts = new RepositoryArtifactProvider(allRemoteArtifactRepositories,
                ArtifactTransferPolicies.forRemoteArtifacts(), remoteAgent);
        MirroringArtifactProvider remoteArtifactCache = MirroringArtifactProvider.createInstance(
                localArtifactRepository, remoteArtifacts, tpConfiguration.getIncludePackedArtifacts(), logger);

        IRawArtifactFileProvider jointArtifacts = new CompositeArtifactProvider(pomDependencyArtifactRepo,
                remoteArtifactCache);

        TargetPlatformImpl targetPlatform = new TargetPlatformImpl(reactorProjects,//
                externalUIs, //
                pomDependenciesContent.getMavenInstallableUnits(), //
                eeResolutionHandler.getResolutionHints(), //
                filter, //
                localMetadataRepository, //
                jointArtifacts, //
                localArtifactRepository, //
                includeLocalMavenRepo,//
                logger);

        eeResolutionHandler.readFullSpecification(targetPlatform.getInstallableUnits());

        // TODO 372780 make jointArtifacts accessible in repo manager and use instead of local artifact repository
        return targetPlatform;
    }

    /**
     * external installable units collected from p2 repositories, .target files and local maven
     * repository
     */
    private LinkedHashSet<IInstallableUnit> gatherExternalInstallableUnits(
            List<TargetDefinitionContent> targetFileContent, TargetPlatformConfigurationStub tpConfig,
            boolean includeLocalMavenRepo) {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (TargetDefinitionContent contentPart : targetFileContent) {
            result.addAll(contentPart.getUnits());
        }

        List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();
        for (MavenRepositoryLocation location : tpConfig.getP2Repositories()) {
            addP2Repository(location, metadataRepositories);
        }
        if (includeLocalMavenRepo) {
            metadataRepositories.add(localMetadataRepository);
        }

        SubMonitor sub = SubMonitor.convert(monitor, metadataRepositories.size() * 200);
        for (IMetadataRepository repository : metadataRepositories) {
            IQueryResult<IInstallableUnit> matches = repository.query(QueryUtil.ALL_UNITS, sub.newChild(100));
            result.addAll(matches.toUnmodifiableSet());
        }
        sub.done();

        if (includeLocalMavenRepo && logger.isDebugEnabled()) {
            IQueryResult<IInstallableUnit> locallyInstalledIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS,
                    null);
            logger.debug("Added " + countElements(locallyInstalledIUs.iterator())
                    + " locally built units to the target platform");
        }

        return result;
    }

    private int countElements(Iterator<?> iterator) {
        int result = 0;
        for (; iterator.hasNext(); iterator.next()) {
            ++result;
        }
        return result;
    }

    private static boolean isPartialIU(IInstallableUnit iu) {
        return Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue();
    }

    private void applyFilters(TargetPlatformFilterEvaluator filter, LinkedHashSet<IInstallableUnit> units,
            Set<IInstallableUnit> reactorProjectUIs, ExecutionEnvironmentResolutionHints eeResolutionHints) {

        Set<String> reactorIUIDs = new HashSet<String>();
        for (IInstallableUnit unit : reactorProjectUIs) {
            reactorIUIDs.add(unit.getId());
        }

        // a.jre/config.a.jre installable units
        // partial installable units
        // installable units shadowed by reactor projects
        Iterator<IInstallableUnit> iter = units.iterator();
        while (iter.hasNext()) {
            IInstallableUnit unit = iter.next();
            if (eeResolutionHints.isNonApplicableEEUnit(unit) || isPartialIU(unit)
                    || reactorIUIDs.contains(unit.getId())) {
                // TODO log
                iter.remove();
                continue;
            }
        }

        // configured filters
        if (filter != null) {
            filter.filterUnits(units);
        }
    }

    private Set<IInstallableUnit> getReactorProjectUIs(List<IReactorArtifactFacade> reactorProjects,
            boolean failOnDuplicateIUs) throws DuplicateReactorIUsException {
        Map<IInstallableUnit, Set<File>> reactorUIs = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<IInstallableUnit, Set<File>>();

        for (IReactorArtifactFacade project : reactorProjects) {
            LinkedHashSet<IInstallableUnit> projectIUs = new LinkedHashSet<IInstallableUnit>();
            projectIUs.addAll(toSet(project.getDependencyMetadata(true), IInstallableUnit.class));
            projectIUs.addAll(toSet(project.getDependencyMetadata(false), IInstallableUnit.class));

            for (IInstallableUnit iu : projectIUs) {
                Set<File> locations = reactorUIs.get(iu);
                if (locations == null) {
                    locations = new LinkedHashSet<File>();
                    reactorUIs.put(iu, locations);
                }
                locations.add(project.getLocation());
                if (locations.size() > 1) {
                    duplicateReactorUIs.put(iu, locations);
                }
            }
        }

        if (failOnDuplicateIUs && !duplicateReactorUIs.isEmpty()) {
            // TODO 392320 we should only fail if IUs with same id and version but different content are found 
            throw new DuplicateReactorIUsException(duplicateReactorUIs);
        }

        return reactorUIs.keySet();
    }

    static <T> Set<T> toSet(Collection<Object> collection, Class<T> targetType) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<T> set = new LinkedHashSet<T>();

        for (Object o : collection) {
            set.add(targetType.cast(o));
        }

        return set;
    }

    private static boolean shouldIncludeLocalMavenRepo(MavenContext context) {
        boolean ignoreLocal = "ignore".equalsIgnoreCase(context.getSessionProperties().getProperty(
                "tycho.localArtifacts"));
        if (ignoreLocal) {
            context.getLogger().debug(
                    "tycho.localArtifacts=" + context.getSessionProperties().getProperty("tycho.localArtifacts")
                            + " -> ignoring locally built artifacts");
        }
        return !ignoreLocal;
    }

}
