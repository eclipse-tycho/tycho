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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.impl.resolver.ClassifiedLocation;
import org.eclipse.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.impl.resolver.LoggingProgressMonitor;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;
import org.eclipse.tycho.p2.target.filters.TargetPlatformFilterEvaluator;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.repository.ProviderOnlyArtifactRepository;
import org.eclipse.tycho.repository.registry.ArtifactRepositoryBlackboard;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;

@SuppressWarnings("restriction")
public class TargetPlatformBuilderImpl implements TargetPlatformBuilder {

    private final MavenLogger logger;

    private final IProgressMonitor monitor;

    private final boolean offline;

    private final IProvisioningAgent remoteAgent;

    private final IMetadataRepositoryManager remoteMetadataRepositoryManager;
    private final IArtifactRepositoryManager remoteArtifactRepositoryManager;
    private IRepositoryIdManager remoteRepositoryIdManager;

    private final TargetDefinitionResolverService targetDefinitionResolverService;

    private boolean includePackedArtifacts;
    private boolean failOnDuplicateIUs = true;

    /**
     * Representation of the target execution environment profile. In case of a custom EE profile,
     * the handler also reads the full specification from the target platform.
     */
    private final ExecutionEnvironmentResolutionHandler eeResolutionHandler;

    /** maven local repository as P2 IArtifactRepository */
    private final LocalArtifactRepository localArtifactRepository;

    /** maven local repository as P2 IMetadataRepository */
    private final LocalMetadataRepository localMetadataRepository;
    private boolean includeLocalMavenRepo;

    public TargetPlatformBuilderImpl(IProvisioningAgent remoteAgent, MavenContext mavenContext,
            TargetDefinitionResolverService targetDefinitionResolverService,
            ExecutionEnvironmentResolutionHandler eeResolutionHandler, LocalArtifactRepository localArtifactRepo,
            LocalMetadataRepository localMetadataRepo) throws ProvisionException {
        this.remoteAgent = remoteAgent;
        this.targetDefinitionResolverService = targetDefinitionResolverService;
        this.logger = mavenContext.getLogger();
        this.monitor = new LoggingProgressMonitor(logger);
        this.includeLocalMavenRepo = shouldIncludeLocalMavenRepo(mavenContext);

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

        this.eeResolutionHandler = eeResolutionHandler;

        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.bundlesPublisher = new TargetPlatformBundlePublisher(localRepositoryRoot, logger);

        // setup p2 views of maven local repository
        this.localArtifactRepository = localArtifactRepo;
        this.localMetadataRepository = localMetadataRepo;

        if (includeLocalMavenRepo) {
            metadataRepositories.add(this.localMetadataRepository);
        }
    }

    // ---------------------------------------------------------------------

    private Map<ClassifiedLocation, IReactorArtifactFacade> reactorProjects = new LinkedHashMap<ClassifiedLocation, IReactorArtifactFacade>();

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<IInstallableUnit, IArtifactFacade>();

    public void addReactorArtifact(IReactorArtifactFacade artifact) {
        ClassifiedLocation key = new ClassifiedLocation(artifact);

//        if (reactorProjects.containsKey(key)) {
//            throw new IllegalStateException();
//        }

        reactorProjects.put(key, artifact);
    }

    public void addArtifactWithExistingMetadata(IArtifactFacade artifact, IArtifactFacade p2MetadataFile) {
        try {
            addMavenArtifact(new ClassifiedLocation(artifact), artifact, readUnits(p2MetadataFile));
        } catch (IOException e) {
            throw new RuntimeException("failed to read p2 metadata", e);
        }
    }

    private Set<IInstallableUnit> readUnits(IArtifactFacade p2MetadataFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(p2MetadataFile.getLocation());
        try {
            MetadataIO io = new MetadataIO();
            return io.readXML(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public void addMavenArtifact(ClassifiedLocation key, IArtifactFacade artifact, Set<IInstallableUnit> units) {
        for (IInstallableUnit unit : units) {
            String classifier = unit.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER);
            if (classifier == null ? key.getClassifier() == null : classifier.equals(key.getClassifier())) {
                mavenInstallableUnits.put(unit, artifact);
                if (logger.isDebugEnabled()) {
                    logger.debug("P2Resolver: artifact "
                            + new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion())
                                    .toString() + " at location " + artifact.getLocation()
                            + " resolves installable unit " + new VersionedId(unit.getId(), unit.getVersion()));
                }
            }
        }
    }

    // ----------------------------------------------------------

    private final TargetPlatformBundlePublisher bundlesPublisher;

    public void publishAndAddArtifactIfBundleArtifact(IArtifactFacade artifact) {
        IInstallableUnit bundleIU = bundlesPublisher.attemptToPublishBundle(artifact);
        if (bundleIU != null)
            addMavenArtifact(new ClassifiedLocation(artifact), artifact, Collections.singleton(bundleIU));
    }

    // ------------------------------------------------------------

    /**
     * All known P2 metadata repositories, including maven local repository unless
     * {@link #includeLocalMavenRepo} is <code>false</code>.
     */
    private final Set<IMetadataRepository> metadataRepositories = new LinkedHashSet<IMetadataRepository>();

    /**
     * All known P2 artifact repositories, NOT including maven local repository.
     */
    private final List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

    public void addP2Repository(MavenRepositoryLocation location) {
        IMetadataRepository metadataRepository = null;
        IArtifactRepository artifactRepository = null;

        try {
            remoteRepositoryIdManager.addMapping(location.getId(), location.getURL());

            // TODO always log that a p2 repository is added to the target platform somewhere; used to be either from p2 or the following line
            // logger.info("Adding repository (cached) " + location.toASCIIString());

            metadataRepository = remoteMetadataRepositoryManager.loadRepository(location.getURL(), monitor);
            metadataRepositories.add(metadataRepository);

            // TODO the agent should return transparently return empty repositories in offline mode
            if (!offline || URIUtil.isFileURI(location.getURL())) {
                artifactRepository = remoteArtifactRepositoryManager.loadRepository(location.getURL(), monitor);
                artifactRepositories.add(artifactRepository);

                forceSingleThreadedDownload(artifactRepository);
            }

            // processPartialIUs( metadataRepository, artifactRepository );
        } catch (ProvisionException e) {
            String idMessage = location.getId() == null ? "" : " with ID '" + location.getId() + "'";
            throw new RuntimeException("Failed to load p2 repository" + idMessage + " from location "
                    + location.getURL(), e);
        }
    }

    // convenience method for tests
    public void addP2Repository(URI location) {
        addP2Repository(new MavenRepositoryLocation(null, location));
    }

    protected void forceSingleThreadedDownload(IArtifactRepository artifactRepository) {
        try {
            if (artifactRepository instanceof SimpleArtifactRepository) {
                OrderedProperties p = getProperties(artifactRepository);
                p.put(org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.PROP_MAX_THREADS,
                        "1");
            } else if (artifactRepository instanceof CompositeArtifactRepository) {
                List<URI> children = ((CompositeArtifactRepository) artifactRepository).getChildren();
                for (URI child : children) {
                    forceSingleThreadedDownload(remoteArtifactRepositoryManager.loadRepository(child, monitor));
                }
            }
        } catch (Exception e) {
            // we've tried
        }
    }

    private OrderedProperties getProperties(IArtifactRepository artifactRepository) throws NoSuchFieldException,
            IllegalAccessException {
        // TODO there should be a better way to modify repository properties
        Field field = AbstractRepository.class.getDeclaredField("properties");
        field.setAccessible(true);
        OrderedProperties p = (OrderedProperties) field.get((SimpleArtifactRepository) artifactRepository);
        return p;
    }

    // ------------------------------------------------------------------------------

    // TODO have other target platform content contributors also add to this list
    private List<TargetPlatformContent> content = new ArrayList<TargetPlatformContent>();

    public void addTargetDefinition(TargetDefinition definition, List<TargetEnvironment> environments)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
        TargetPlatformContent targetFileContent = targetDefinitionResolverService.getTargetDefinitionContent(
                definition, environments, eeResolutionHandler.getResolutionHints(), remoteAgent);
        content.add(targetFileContent);

        if (logger.isDebugEnabled()) {
            logger.debug("Added " + targetFileContent.getUnits().size()
                    + " units, the content of the target definition file, to the target platform");
        }
    }

    // --------------------------------------------------------------------------------
    // creating copy&paste from org.eclipse.equinox.internal.p2.repository.Credentials.forLocation(URI, boolean,
    // AuthenticationInfo)

    // -------------------------------------------------------------------------

    private File projectLocation;

    // TODO 372780 get rid of this method
    public void setProjectLocation(File projectLocation) {
        this.projectLocation = projectLocation;
    }

    public P2TargetPlatform buildTargetPlatform() {
        // TODO 372780 get rid of this special handling of pomDependency artifacts: there should be one p2 artifact repo view on the target platform
        IRawArtifactFileProvider pomDependencyArtifactRepo = bundlesPublisher.getArtifactRepoOfPublishedBundles();
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey.forResolutionContextArtifacts(projectLocation);
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, new ProviderOnlyArtifactRepository(
                pomDependencyArtifactRepo, Activator.getProvisioningAgent(), blackboardKey.toURI()));
        logger.debug("Registered artifact repository " + blackboardKey);

        Set<IInstallableUnit> reactorProjectUIs = getReactorProjectUIs();

        TargetPlatformFilterEvaluator filter = !iuFilters.isEmpty() ? new TargetPlatformFilterEvaluator(iuFilters,
                logger) : null;

        LinkedHashSet<IInstallableUnit> externalUIs = gatherExternalInstallableUnits(monitor);
        applyFilters(filter, externalUIs, reactorProjectUIs);

        LinkedHashSet<IInstallableUnit> mavenIUs = gatherMavenInstallableUnits();
        applyFilters(filter, mavenIUs, reactorProjectUIs);

        List<URI> allRemoteArtifactRepositories = new ArrayList<URI>();
        for (IArtifactRepository artifactRepository : artifactRepositories) {
            allRemoteArtifactRepositories.add(artifactRepository.getLocation());
        }
        for (TargetPlatformContent contentPart : content) {
            allRemoteArtifactRepositories.addAll(contentPart.getArtifactRepositoryLocations());
        }

        TargetPlatformImpl targetPlatform = new TargetPlatformImpl(reactorProjects.values(),//
                externalUIs, //
                mavenInstallableUnits, //
                eeResolutionHandler.getResolutionHints(), //
                filter, //
                localMetadataRepository, //
                allRemoteArtifactRepositories, //
                localArtifactRepository, //
                remoteAgent, //
                includePackedArtifacts, //
                includeLocalMavenRepo,//
                logger);

        eeResolutionHandler.readFullSpecification(targetPlatform.getInstallableUnits());

        return targetPlatform;
    }

    // -------------------------------------------------------------------------

    /**
     * external installable units collevted from p2 repositories, .target files and local maven
     * repository
     */
    private LinkedHashSet<IInstallableUnit> gatherExternalInstallableUnits(IProgressMonitor monitor) {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (TargetPlatformContent contentPart : content) {
            result.addAll(contentPart.getUnits());
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

    /**
     * installable units from pom-first artifacts, excluding reactor project
     */
    private LinkedHashSet<IInstallableUnit> gatherMavenInstallableUnits() {
        return new LinkedHashSet<IInstallableUnit>(mavenInstallableUnits.keySet());
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

    // -------------------------------------------------------------------------

    private List<TargetPlatformFilter> iuFilters = new ArrayList<TargetPlatformFilter>();

    public void addFilters(List<TargetPlatformFilter> filters) {
        this.iuFilters.addAll(filters);
    }

    private void applyFilters(TargetPlatformFilterEvaluator filter, LinkedHashSet<IInstallableUnit> units,
            Set<IInstallableUnit> reactorProjectUIs) {

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
            if (eeResolutionHandler.getResolutionHints().isNonApplicableEEUnit(unit) || isPartialIU(unit)
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

    // -------------------------------------------------------------------------------

    private Set<IInstallableUnit> getReactorProjectUIs() throws DuplicateReactorIUsException {
        Map<IInstallableUnit, Set<File>> reactorUIs = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<IInstallableUnit, Set<File>>();

        for (IReactorArtifactFacade project : reactorProjects.values()) {
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

    public void setIncludePackedArtifacts(boolean include) {
        this.includePackedArtifacts = include;
    }

    public void setFailOnDuplicateIUs(boolean failOnDuplicateIUs) {
        this.failOnDuplicateIUs = failOnDuplicateIUs;
    }

    public void setIncludeLocalMavenRepo(boolean includeLocalMavenRepo) {
        this.includeLocalMavenRepo = includeLocalMavenRepo;
        if (includeLocalMavenRepo) {
            metadataRepositories.add(localMetadataRepository);
        } else {
            metadataRepositories.remove(localMetadataRepository);
        }
    }

    static boolean shouldIncludeLocalMavenRepo(MavenContext context) {
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
