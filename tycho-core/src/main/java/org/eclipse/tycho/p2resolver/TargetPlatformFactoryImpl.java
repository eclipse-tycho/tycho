/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.ee.impl.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.core.osgitools.ClasspathReader;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.core.resolver.target.DuplicateReactorIUsException;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.core.resolver.target.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.core.shared.DuplicateFilteringLoggingProgressMonitor;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.model.classpath.JUnitBundle;
import org.eclipse.tycho.model.classpath.JUnitClasspathContainerEntry;
import org.eclipse.tycho.model.classpath.ProjectClasspathEntry;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;
import org.eclipse.tycho.p2.repository.ArtifactRepositoryBlackboard;
import org.eclipse.tycho.p2.repository.ArtifactTransferPolicies;
import org.eclipse.tycho.p2.repository.CompositeArtifactProvider;
import org.eclipse.tycho.p2.repository.FileRepositoryArtifactProvider;
import org.eclipse.tycho.p2.repository.LazyArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.MirroringArtifactProvider;
import org.eclipse.tycho.p2.repository.ProviderOnlyArtifactRepository;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.p2.repository.RepositoryBlackboardKey;
import org.eclipse.tycho.p2.resolver.BundlePublisher;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;
import org.eclipse.tycho.p2tools.copiedfromp2.QueryableArray;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter;
import org.osgi.framework.BundleException;

public class TargetPlatformFactoryImpl implements TargetPlatformFactory {

    private static final Version DEFAULT_P2_ADVICE_VERSION = Version.parseVersion("1.0.0.qualifier");

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
    private TychoProjectManager projectManager;
    private MavenBundleResolver mavenBundleResolver;

    public TargetPlatformFactoryImpl(MavenContext mavenContext, IProvisioningAgent remoteAgent,
            LocalArtifactRepository localArtifactRepo, LocalMetadataRepository localMetadataRepo,
            TargetDefinitionResolverService targetDefinitionResolverService, IRepositoryIdManager repositoryIdManager,
            TychoProjectManager projectManager, MavenBundleResolver mavenBundleResolver) {
        this.mavenContext = mavenContext;
        this.projectManager = projectManager;
        this.mavenBundleResolver = mavenBundleResolver;
        this.logger = mavenContext.getLogger();
        this.monitor = new DuplicateFilteringLoggingProgressMonitor(logger); // entails that this class is not thread-safe

        this.remoteAgent = remoteAgent;
        this.remoteRepositoryIdManager = repositoryIdManager;
        this.offline = mavenContext.isOffline();

        this.remoteMetadataRepositoryManager = remoteAgent.getService(IMetadataRepositoryManager.class);
        if (remoteMetadataRepositoryManager == null) {
            throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
        }

        this.remoteArtifactRepositoryManager = remoteAgent.getService(IArtifactRepositoryManager.class);
        if (remoteArtifactRepositoryManager == null) {
            throw new IllegalStateException("No artifact repository manager found"); //$NON-NLS-1$
        }

        this.localMetadataRepository = localMetadataRepo;
        this.localArtifactRepository = localArtifactRepo;
        this.targetDefinitionResolverService = targetDefinitionResolverService;
    }

    @Override
    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects) {
        return createTargetPlatform(tpConfiguration,
                ExecutionEnvironmentResolutionHandler.adapt(eeConfiguration, logger), reactorProjects);
    }

    @Override
    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects,
            ReactorProject project) {
        return createTargetPlatform(tpConfiguration,
                ExecutionEnvironmentResolutionHandler.adapt(eeConfiguration, logger), reactorProjects, project);
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
            ExecutionEnvironmentResolutionHandler eeResolutionHandler, List<ReactorProject> reactorProjects) {
        return createTargetPlatform(tpConfiguration, eeResolutionHandler, reactorProjects, null);
    }

    public P2TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentResolutionHandler eeResolutionHandler, List<ReactorProject> reactorProjects,
            ReactorProject project) {

        List<TargetDefinitionContent> targetFileContent = resolveTargetDefinitions(tpConfiguration,
                eeResolutionHandler.getResolutionHints());

        Set<MavenRepositoryLocation> completeRepositories = tpConfiguration.getP2Repositories();
        registerRepositoryIDs(completeRepositories);

        // collect & process metadata
        Set<URI> artifactRepositories = new LinkedHashSet<>();
        boolean includeLocalMavenRepo = !tpConfiguration.getIgnoreLocalArtifacts();
        Set<IInstallableUnit> externalUIs = gatherExternalInstallableUnits(completeRepositories, targetFileContent,
                includeLocalMavenRepo, artifactRepositories,
                tpConfiguration.getIncludeRefererenced() == ReferencedRepositoryMode.include);

        //add maven junit bundles...
        List<MavenArtifactKey> junitBundles = getMissingJunitBundles(project, externalUIs);
        MissingBundlesArtifactFileProvider extraMavenBundles = new MissingBundlesArtifactFileProvider();
        for (MavenArtifactKey mavenArtifactKey : junitBundles) {
            Optional<ResolvedArtifactKey> mavenBundle = mavenBundleResolver.resolveMavenBundle(
                    project.adapt(MavenProject.class), project.adapt(MavenSession.class), mavenArtifactKey);
            mavenBundle.flatMap(key -> {
                File bundleFile = key.getLocation();
                try {
                    MavenPropertiesAdvice advice = new MavenPropertiesAdvice(mavenArtifactKey.getGroupId(),
                            mavenArtifactKey.getArtifactId(), key.getVersion());
                    Optional<IInstallableUnit> iu = BundlePublisher.getBundleIU(bundleFile, null, advice);
                    IInstallableUnit unit = iu.orElse(null);
                    if (unit != null) {
                        InstallableUnitDescription description = new InstallableUnitDescription();
                        unit.getProperties().forEach((k, v) -> description.setProperty(k, v));
                        description.setId(unit.getId());
                        description.setVersion(unit.getVersion());
                        description.addProvidedCapabilities(unit.getProvidedCapabilities());
                        if (!mavenArtifactKey.getId().equals(unit.getId())) {
                            IProvidedCapability cap = MetadataFactory.createProvidedCapability(
                                    "org.eclipse.equinox.p2.iu", mavenArtifactKey.getId(), unit.getVersion());
                            description.addProvidedCapabilities(List.of(cap));
                        }
                        IArtifactKey[] artifactKeys = unit.getArtifacts().toArray(IArtifactKey[]::new);
                        description.setArtifacts(artifactKeys);
                        for (IArtifactKey mavenkey : artifactKeys) {
                            extraMavenBundles.add(mavenkey, bundleFile);
                        }
                        return Optional.of(MetadataFactory.createInstallableUnit(description));
                    }
                } catch (IOException e) {
                } catch (BundleException e) {
                }
                return Optional.empty();
            }).ifPresent(externalUIs::add);
        }
        //add p2.inf extra units from all projects...
        for (ReactorProject reactorProject : Objects.requireNonNullElse(reactorProjects, List.<ReactorProject> of())) {
            gatherP2InfUnits(reactorProject, externalUIs);
        }

        Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectUIs = getPreliminaryReactorProjectUIs(
                reactorProjects);

        List<TargetPlatformFilter> iuFilters = tpConfiguration.getFilters();
        TargetPlatformFilterEvaluator filter = !iuFilters.isEmpty()
                ? new TargetPlatformFilterEvaluator(iuFilters, logger)
                : null;

        applyConfiguredFilter(filter, reactorProjectUIs.keySet());
        Set<IInstallableUnit> shadowed = new HashSet<>();
        applyFilters(filter, externalUIs, reactorProjectUIs.keySet(), eeResolutionHandler.getResolutionHints(),
                shadowed);

        IRawArtifactFileProvider externalArtifactFileProvider = createExternalArtifactProvider(artifactRepositories,
                targetFileContent, extraMavenBundles);
        PreliminaryTargetPlatformImpl targetPlatform = new PreliminaryTargetPlatformImpl(reactorProjectUIs, //
                externalUIs, //
                eeResolutionHandler.getResolutionHints(), //
                filter, //
                localMetadataRepository, //
                externalArtifactFileProvider, //
                localArtifactRepository, //
                includeLocalMavenRepo, //
                logger, shadowed, remoteAgent);
        eeResolutionHandler.readFullSpecification(targetPlatform.getInstallableUnits());

        return targetPlatform;
    }

    private void gatherP2InfUnits(ReactorProject reactorProject, Set<IInstallableUnit> externalUIs) {
        if (reactorProject == null) {
            return;
        }

        AdviceFileAdvice advice;
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(reactorProject.getPackaging())) {
            advice = new AdviceFileAdvice(reactorProject.getArtifactId(), getVersion(reactorProject),
                    new Path(reactorProject.getBasedir().getAbsolutePath()), AdviceFileAdvice.BUNDLE_ADVICE_FILE);
        } else if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(reactorProject.getPackaging())) {
            advice = new AdviceFileAdvice(reactorProject.getArtifactId(), getVersion(reactorProject),
                    new Path(reactorProject.getBasedir().getAbsolutePath()), new Path("p2.inf"));
        } else {
            //not a project with advice...
            return;
        }
        if (advice.containsAdvice()) {
            InstallableUnitDescription[] descriptions = advice.getAdditionalInstallableUnitDescriptions(null);
            if (descriptions != null && descriptions.length > 0) {
                for (InstallableUnitDescription desc : descriptions) {
                    externalUIs.add(MetadataFactory.createInstallableUnit(desc));
                }
            }
        }
    }

    private Version getVersion(ReactorProject reactorProject) {
        if (projectManager == null) {
            return DEFAULT_P2_ADVICE_VERSION;
        }
        try {
            return projectManager.getArtifactKey(reactorProject).map(key -> Version.parseVersion(key.getVersion()))
                    .orElseGet(() -> DEFAULT_P2_ADVICE_VERSION);
        } catch (IllegalArgumentException ex) {
            return DEFAULT_P2_ADVICE_VERSION;
        }
    }

    private List<MavenArtifactKey> getMissingJunitBundles(ReactorProject project, Set<IInstallableUnit> externalUIs) {
        List<MavenArtifactKey> missing = new ArrayList<>();
        if (projectManager != null) {
            Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
            tychoProject.filter(OsgiBundleProject.class::isInstance).map(OsgiBundleProject.class::cast)
                    .map(obp -> obp.getEclipsePluginProject(project)).ifPresent(eclipseProject -> {
                        Collection<ProjectClasspathEntry> entries = eclipseProject.getClasspathEntries();
                        for (ProjectClasspathEntry entry : entries) {
                            if (entry instanceof JUnitClasspathContainerEntry junit) {
                                IQueryable<IInstallableUnit> queriable = new QueryableArray(externalUIs, false);
                                Collection<JUnitBundle> artifacts = junit.getArtifacts();
                                for (JUnitBundle bundle : artifacts) {
                                    MavenArtifactKey maven = ClasspathReader.toMaven(bundle);
                                    VersionRange range = new VersionRange(maven.getVersion());
                                    IQuery<IInstallableUnit> query = ArtifactTypeHelper.createQueryFor(maven.getType(),
                                            maven.getId(), range);
                                    IQueryResult<IInstallableUnit> result = queriable
                                            .query(QueryUtil.createLatestQuery(query), monitor);
                                    if (result.isEmpty()) {
                                        missing.add(maven);
                                    }
                                }
                            }
                        }
                    });
        }
        return missing;
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
                    tpConfiguration.getIncludeSourceMode(), tpConfiguration.getIncludeRefererenced(), remoteAgent);
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

    /**
     * External installable units collected from p2 repositories, .target files and local Maven
     * repository.
     * 
     * @param artifactRepositories
     */
    private LinkedHashSet<IInstallableUnit> gatherExternalInstallableUnits(
            Set<MavenRepositoryLocation> completeRepositories, List<TargetDefinitionContent> targetDefinitionsContent,
            boolean includeLocalMavenRepo, Set<URI> artifactRepositories, boolean includeReferences) {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<>();

        for (TargetDefinitionContent targetDefinitionContent : targetDefinitionsContent) {
            IQueryResult<IInstallableUnit> queryResult = targetDefinitionContent.query(QueryUtil.ALL_UNITS, monitor);
            result.addAll(queryResult.toUnmodifiableSet());
        }

        List<IMetadataRepository> metadataRepositories = new ArrayList<>();
        Set<URI> loaded = new HashSet<>();
        for (MavenRepositoryLocation location : completeRepositories) {
            artifactRepositories.add(location.getURL());
            try {
                loadMetadataRepository(location, metadataRepositories, loaded, artifactRepositories, includeReferences);
            } catch (ProvisionException e) {
                String idMessage = location.getId() == null ? "" : " with ID '" + location.getId() + "'";
                throw new RuntimeException(
                        "Failed to load p2 repository" + idMessage + " from location " + location.getURL(), e);
            }
        }
        if (includeLocalMavenRepo) {
            metadataRepositories.add(localMetadataRepository);
        }

        for (IMetadataRepository repository : metadataRepositories) {
            IQueryResult<IInstallableUnit> matches = repository.query(QueryUtil.ALL_UNITS, monitor);
            result.addAll(matches.toUnmodifiableSet());
        }

        if (includeLocalMavenRepo && logger.isDebugEnabled()) {
            IQueryResult<IInstallableUnit> locallyInstalledIUs = localMetadataRepository.query(QueryUtil.ALL_UNITS,
                    null);
            logger.debug("Added " + countElements(locallyInstalledIUs.iterator())
                    + " locally built units to the target platform");
        }

        return result;
    }

    private void loadMetadataRepository(MavenRepositoryLocation location,
            List<IMetadataRepository> metadataRepositories, Set<URI> loaded, Set<URI> artifactRepositories,
            boolean includeReferences) throws ProvisionException {
        if (loaded.add(location.getURL().normalize())) {
            IMetadataRepository repository = remoteMetadataRepositoryManager.loadRepository(location.getURL(), monitor);
            metadataRepositories.add(repository);
            if (includeReferences) {
                for (IRepositoryReference reference : repository.getReferences()) {
                    if ((reference.getOptions() | IRepository.ENABLED) != 0) {
                        if (reference.getType() == IRepository.TYPE_METADATA) {
                            try {
                                loadMetadataRepository(
                                        new MavenRepositoryLocation(reference.getNickname(), reference.getLocation()),
                                        metadataRepositories, loaded, artifactRepositories, includeReferences);
                            } catch (ProvisionException e) {
                                logger.warn("Loading referenced repository failed: " + e.getMessage(),
                                        logger.isDebugEnabled() ? e : null);
                            }
                        } else if (reference.getType() == IRepository.TYPE_ARTIFACT) {
                            artifactRepositories.add(reference.getLocation());
                        }
                    }
                }
            }

        }
    }

    private static final class SortedRepositories {

        private SortedRepositories(List<FileArtifactRepository> local, List<IArtifactRepository> remote) {
            this.remoteRepositories = remote;
            this.localRepositories = local;
        }

        /**
         * Sorts the input repositories to separate the "local" ones that do not require
         * mirroring/caching (for example ArtifactRepositories mapping a local Maven artifacts) and
         * the remote ones that typically will require fetch/cache. The repositories are "expanded"
         * or "flattened": {@link ListCompositeArtifactRepository} are not kept as it, but are
         * considered as a list of artifact repositories to sort.
         * 
         * @param repositories
         * @return remote vs local repositories
         */
        public static SortedRepositories sort(List<IArtifactRepository> repositories) {
            List<IArtifactRepository> remote = new ArrayList<>();
            List<FileArtifactRepository> local = new ArrayList<>();
            for (IArtifactRepository repo : repositories) {
                if (repo instanceof ListCompositeArtifactRepository list) {
                    SortedRepositories children = SortedRepositories.sort(list.artifactRepositories);
                    remote.addAll(children.remoteRepositories);
                    local.addAll(children.localRepositories);
                } else if (repo instanceof FileArtifactRepository fileArtifactRepo) {
                    local.add(fileArtifactRepo);
                } else {
                    remote.add(repo);
                }
            }
            return new SortedRepositories(local, remote);
        }

        public final List<IArtifactRepository> remoteRepositories;
        public final List<FileArtifactRepository> localRepositories;

    }

    /**
     * Provider for all target platform artifacts from outside the reactor.
     */
    private IRawArtifactFileProvider createExternalArtifactProvider(Set<URI> completeRepositories,
            List<TargetDefinitionContent> targetDefinitionsContent, IRawArtifactFileProvider extraMavenBundles) {
        SortedRepositories repos = SortedRepositories
                .sort(targetDefinitionsContent.stream().map(TargetDefinitionContent::getArtifactRepository).toList());
        RepositoryArtifactProvider remoteArtifactProvider = createRemoteArtifactProvider(completeRepositories,
                repos.remoteRepositories);
        MirroringArtifactProvider remoteArtifactProviderWithCache = MirroringArtifactProvider
                .createInstance(localArtifactRepository, remoteArtifactProvider, mavenContext);

        return new CompositeArtifactProvider(
                new FileRepositoryArtifactProvider(repos.localRepositories,
                        ArtifactTransferPolicies.forLocalArtifacts()), //
                remoteArtifactProviderWithCache, extraMavenBundles);
    }

    /**
     * Provider for the target platform artifacts not yet available in the local Maven repository.
     */
    private RepositoryArtifactProvider createRemoteArtifactProvider(Set<URI> mavenRepositories,
            List<IArtifactRepository> repos) {
        List<IArtifactRepository> artifactRepositories = new ArrayList<>();

        for (URI location : mavenRepositories) {
            if (!offline || URIUtil.isFileURI(location)) {
                artifactRepositories.add(
                        new LazyArtifactRepository(remoteAgent, location, RepositoryArtifactProvider::loadRepository));
            }
        }

        artifactRepositories.addAll(repos);
        return new RepositoryArtifactProvider(artifactRepositories, ArtifactTransferPolicies.forLocalArtifacts());
    }

    private Map<IInstallableUnit, ReactorProjectIdentities> getPreliminaryReactorProjectUIs(
            List<ReactorProject> reactorProjects) throws DuplicateReactorIUsException {
        if (reactorProjects == null) {
            return Collections.emptyMap();
        }

        Map<IInstallableUnit, ReactorProjectIdentities> reactorUIs = new HashMap<>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<>();

        for (ReactorProject project : reactorProjects) {

            Set<IInstallableUnit> projectIUs = getPreliminaryReactorProjectUIs(project);
            if (projectIUs == null || projectIUs.isEmpty()) {
                continue;
            }
            for (IInstallableUnit iu : projectIUs) {
                ReactorProjectIdentities identities = project.getIdentities();
                ReactorProjectIdentities otherOrigin = reactorUIs.put(iu, identities);
                if (otherOrigin != null && !otherOrigin.equals(identities)) {
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

    private Set<IInstallableUnit> getPreliminaryReactorProjectUIs(ReactorProject project) {
        String packaging = project.getPackaging();
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging) || PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
            File artifact = project.getArtifact();
            if (artifact != null && artifact.isFile()) {
                //the project was already build, use the seed units as they include anything maybe updated by p2-metadata mojo
                return project.getDependencyMetadata(DependencyMetadataType.SEED);
            }
        }
        return project.getDependencyMetadata(DependencyMetadataType.INITIAL);
    }

    private void applyFilters(TargetPlatformFilterEvaluator filter, Collection<IInstallableUnit> collectionToModify,
            Set<IInstallableUnit> reactorProjectUIs, ExecutionEnvironmentResolutionHints eeResolutionHints,
            Set<IInstallableUnit> shadowedIus) {

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
            boolean shaddowed = reactorIUIDs.contains(unit.getId());
            if (eeResolutionHints.isNonApplicableEEUnit(unit) || isPartialIU(unit) || shaddowed) {
                // TODO debug log output?
                iter.remove();
                if (shaddowed) {
                    shadowedIus.add(unit);
                }
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
        return Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU));
    }

    static int countElements(Iterator<?> iterator) {
        int result = 0;
        for (; iterator.hasNext(); iterator.next()) {
            ++result;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public P2TargetPlatform createTargetPlatformWithUpdatedReactorContent(TargetPlatform baseTargetPlatform,
            List<?> upstreamProjectResults, PomDependencyCollector pomDependencies) {
        PomDependencyCollectorImpl pomDependenciesContent;
        if (pomDependencies instanceof PomDependencyCollectorImpl source) {
            pomDependenciesContent = source;
        } else {
            logger.debug("Using empty PomDependencyCollector instead of given = " + pomDependencies);
            pomDependenciesContent = new PomDependencyCollectorImpl(mavenContext.getLogger().adapt(Logger.class), null,
                    remoteAgent);
        }
        return createTargetPlatformWithUpdatedReactorUnits(baseTargetPlatform,
                extractProjectResultIUs((List<PublishingRepository>) upstreamProjectResults),
                getProjectArtifactProviders((List<PublishingRepository>) upstreamProjectResults),
                pomDependenciesContent);
    }

    public P2TargetPlatform createTargetPlatformWithUpdatedReactorUnits(TargetPlatform baseTargetPlatform,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorUnits,
            List<IRawArtifactFileProvider> reactorArtifacts, PomDependencyCollector pomDependencyCollector) {
        if (!(baseTargetPlatform instanceof PreliminaryTargetPlatformImpl)) {
            throw new IllegalArgumentException(
                    "Base target platform must be an instance of PreliminaryTargetPlatformImpl; was: "
                            + baseTargetPlatform);
        }
        if (!(pomDependencyCollector instanceof PomDependencyCollectorImpl)) {
            throw new IllegalArgumentException(
                    "PomDependencyCollector must be an instance of PomDependencyCollectorImpl; was: "
                            + pomDependencyCollector);
        }
        return createTargetPlatformWithUpdatedReactorUnits((PreliminaryTargetPlatformImpl) baseTargetPlatform,
                reactorUnits, reactorArtifacts, (PomDependencyCollectorImpl) pomDependencyCollector);
    }

    P2TargetPlatform createTargetPlatformWithUpdatedReactorUnits(PreliminaryTargetPlatformImpl preliminaryTP,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorUnitsMap,
            List<IRawArtifactFileProvider> reactorArtifacts, PomDependencyCollectorImpl pomDependencyCollector) {
        LinkedHashSet<IInstallableUnit> allUnits = preliminaryTP.getExternalUnits();
        TargetPlatformFilterEvaluator configuredFilters = preliminaryTP.getFilter();
        // external units are already filtered, only reactor units need to be filtered again
        if (reactorUnitsMap != null) {
            allUnits.addAll(filterUnits(configuredFilters, reactorUnitsMap.keySet()));
        } else {
            reactorUnitsMap = new LinkedHashMap<>();
        }

        IRawArtifactFileProvider pomDependencyArtifactRepo = pomDependencyCollector.getArtifactRepoOfPublishedBundles();
        IRawArtifactFileProvider jointArtifacts = createJointArtifactProvider(reactorArtifacts,
                preliminaryTP.getExternalArtifacts(), pomDependencyArtifactRepo);
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey
                .forResolutionContextArtifacts(pomDependencyCollector.getProjectLocation());
        ProviderOnlyArtifactRepository targetPlatformArtifactRepository = new ProviderOnlyArtifactRepository(
                jointArtifacts, remoteAgent, blackboardKey.toURI());
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, targetPlatformArtifactRepository);
        logger.debug("Registered artifact repository " + blackboardKey);
        allUnits.addAll(filterUnits(configuredFilters, pomDependencyCollector.gatherMavenInstallableUnits()));
        Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = pomDependencyCollector
                .getMavenInstallableUnits();
        for (Entry<IInstallableUnit, IArtifactFacade> entry : mavenInstallableUnits.entrySet()) {
            IArtifactFacade value = entry.getValue();
            if (value instanceof ReactorProjectFacade projectFacade) {
                reactorUnitsMap.put(entry.getKey(), projectFacade.getReactorProject().getIdentities());
            }
        }
        return new FinalTargetPlatformImpl(allUnits, preliminaryTP.getEEResolutionHints(), jointArtifacts,
                localArtifactRepository, mavenInstallableUnits, reactorUnitsMap, targetPlatformArtifactRepository,
                Set.copyOf(preliminaryTP.getShadowed()));
    }

    protected Collection<? extends IInstallableUnit> filterUnits(TargetPlatformFilterEvaluator configuredFilters,
            Collection<? extends IInstallableUnit> units) {
        if (units.isEmpty() || configuredFilters == null) {
            return units;
        }
        List<IInstallableUnit> filteredUnits = new LinkedList<>(units);
        configuredFilters.filterUnits(filteredUnits);
        return filteredUnits;
    }

    private CompositeArtifactProvider createJointArtifactProvider(List<IRawArtifactFileProvider> reactorArtifacts,
            IRawArtifactFileProvider externalArtifacts, IRawArtifactFileProvider pomDependencyArtifactRepo) {
        // prefer artifacts from the reactor
        return new CompositeArtifactProvider(reactorArtifacts,
                Arrays.asList(externalArtifacts, pomDependencyArtifactRepo));
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
