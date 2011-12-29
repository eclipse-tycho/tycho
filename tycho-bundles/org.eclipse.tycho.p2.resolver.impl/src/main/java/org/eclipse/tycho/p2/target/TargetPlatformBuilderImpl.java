/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.ClassifiedLocation;
import org.eclipse.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.impl.resolver.LoggingProgressMonitor;
import org.eclipse.tycho.p2.impl.resolver.P2RepositoryCache;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.eclipse.tycho.repository.registry.impl.ArtifactRepositoryBlackboard;

@SuppressWarnings("restriction")
public class TargetPlatformBuilderImpl implements TargetPlatformBuilder {

    private final MavenLogger logger;

    private final IProgressMonitor monitor;

    private final boolean offline;

    private final IProvisioningAgent agent;

    private final boolean disableP2Mirrors;

    /**
     * Target execution environment profile name or null to use system default profile name.
     */
    private final String executionEnvironment;

    /** maven local repository as P2 IArtifactRepository */
    private final LocalArtifactRepository localArtifactRepository;

    /** maven local repository as P2 IMetadataRepository */
    private final LocalMetadataRepository localMetadataRepository;

    public TargetPlatformBuilderImpl(IProvisioningAgent agent, MavenContext mavenContext, String executionEnvironment,
            LocalRepositoryP2Indices localRepositoryIndices, boolean disableP2Mirrors) {
        this.agent = agent;
        this.logger = mavenContext.getLogger();
        this.monitor = new LoggingProgressMonitor(logger);

        this.metadataRepositoryManager = (IMetadataRepositoryManager) agent
                .getService(IMetadataRepositoryManager.SERVICE_NAME);
        if (metadataRepositoryManager == null) {
            throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
        }

        this.artifactRepositoryManager = (IArtifactRepositoryManager) agent
                .getService(IArtifactRepositoryManager.SERVICE_NAME);
        if (artifactRepositoryManager == null) {
            throw new IllegalStateException("No artifact repository manager found"); //$NON-NLS-1$
        }

        this.repositoryCache = (P2RepositoryCache) agent.getService(P2RepositoryCache.SERVICE_NAME);
        if (repositoryCache == null) {
            throw new IllegalStateException("No Tycho p2 reposiutory cache found");
        }

        this.offline = mavenContext.isOffline();

        this.disableP2Mirrors = disableP2Mirrors;

        // TODO 364134 make this a setter - this property is side-effect free (i.e. it has no effect before gatherAvailableUnits)
        this.executionEnvironment = executionEnvironment;

        File localRepositoryRoot = mavenContext.getLocalRepositoryRoot();
        this.bundlesPublisher = new TargetPlatformBundlePublisher(localRepositoryRoot, logger);

        // setup p2 views of maven local repository
        URI uri = localRepositoryRoot.toURI();

        LocalArtifactRepository localRepository = (LocalArtifactRepository) repositoryCache.getArtifactRepository(uri);
        LocalMetadataRepository localMetadataRepository = (LocalMetadataRepository) repositoryCache
                .getMetadataRepository(uri);

        if (localRepository == null || localMetadataRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(localRepositoryRoot);
            TychoRepositoryIndex metadataIndex = localRepositoryIndices.getMetadataIndex();
            localRepository = new LocalArtifactRepository(localRepositoryIndices, contentLocator);
            localMetadataRepository = new LocalMetadataRepository(uri, metadataIndex, contentLocator);
            repositoryCache.putRepository(uri, localMetadataRepository, localRepository);
        }

        metadataRepositories.add(localMetadataRepository);

        this.localMetadataRepository = localMetadataRepository;
        this.localArtifactRepository = localRepository;
    }

    // ---------------------------------------------------------------------

    private Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectIUs = new HashMap<ClassifiedLocation, Set<IInstallableUnit>>();

    private Map<ClassifiedLocation, Set<IInstallableUnit>> reactorProjectSecondaryIUs = new HashMap<ClassifiedLocation, Set<IInstallableUnit>>();

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<IInstallableUnit, IArtifactFacade>();

    private Set<String> reactorInstallableUnitIds = new HashSet<String>();

    public void addReactorArtifact(IReactorArtifactFacade artifact) {
        addReactorProjectIUs(artifact, reactorProjectIUs, true);
        addReactorProjectIUs(artifact, reactorProjectSecondaryIUs, false);
    }

    private void addReactorProjectIUs(IReactorArtifactFacade artifact,
            Map<ClassifiedLocation, Set<IInstallableUnit>> projectIUs, boolean primary) {
        Set<IInstallableUnit> units = toSet(artifact.getDependencyMetadata(primary), IInstallableUnit.class);

        ClassifiedLocation key = new ClassifiedLocation(artifact);
        projectIUs.put(key, units);
        addMavenArtifact(key, artifact, units);

        for (IInstallableUnit unit : units) {
            reactorInstallableUnitIds.add(unit.getId());
        }
    }

    private static <T> Set<T> toSet(Collection<Object> collection, Class<T> targetType) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<T> set = new LinkedHashSet<T>();

        for (Object o : collection) {
            set.add(targetType.cast(o));
        }

        return set;
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

    /**
     * Returns an {@link IArtifactRepository} instance containing those artifacts in the resolution
     * context which are not in the normal p2 view of the local Maven repository.
     * 
     * @see TargetPlatformBuilderImpl#downloadArtifacts(Collection)
     */
    private IArtifactRepository getSupplementaryArtifactRepository() {
        return bundlesPublisher.getArtifactRepoOfPublishedBundles();
    }

    // ------------------------------------------------------------

    /**
     * All known P2 metadata repositories, including maven local repository
     */
    private final List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();

    /**
     * All known P2 artifact repositories, NOT including maven local repository.
     */
    private final List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

    public void addP2Repository(URI location) {
        IMetadataRepository metadataRepository = null;
        IArtifactRepository artifactRepository = null;

        // check metadata cache, first
        metadataRepository = (IMetadataRepository) repositoryCache.getMetadataRepository(location);
        artifactRepository = (IArtifactRepository) repositoryCache.getArtifactRepository(location);
        if (metadataRepository != null && (offline || artifactRepository != null)) {
            // cache hit
            metadataRepositories.add(metadataRepository);
            if (artifactRepository != null) {
                artifactRepositories.add(artifactRepository);
            }
            logger.info("Adding repository (cached) " + location.toASCIIString());
            return;
        }

        try {
            metadataRepository = metadataRepositoryManager.loadRepository(location, monitor);
            metadataRepositories.add(metadataRepository);

            if (!offline || URIUtil.isFileURI(location)) {
                artifactRepository = artifactRepositoryManager.loadRepository(location, monitor);
                artifactRepositories.add(artifactRepository);

                forceSingleThreadedDownload(artifactRepository);
                if (disableP2Mirrors) {
                    forceMirrorsDisabled(artifactRepository);
                }
            }

            repositoryCache.putRepository(location, metadataRepository, artifactRepository);

            // processPartialIUs( metadataRepository, artifactRepository );
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
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
                    forceSingleThreadedDownload(artifactRepositoryManager.loadRepository(child, monitor));
                }
            }
        } catch (Exception e) {
            // we've tried
        }
    }

    private void forceMirrorsDisabled(IArtifactRepository artifactRepository) throws ProvisionException {
        if (artifactRepository instanceof SimpleArtifactRepository) {
            try {
                OrderedProperties p = getProperties(artifactRepository);
                p.remove(org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.PROP_MIRRORS_URL);
            } catch (Exception e) {
                throw new RuntimeException("Could not disable p2 mirrors", e);
            }
        } else if (artifactRepository instanceof CompositeArtifactRepository) {
            for (URI child : ((CompositeArtifactRepository) artifactRepository).getChildren()) {
                forceMirrorsDisabled(artifactRepositoryManager.loadRepository(child, monitor));
            }
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

    public void addTargetDefinition(TargetDefinition definition, List<Map<String, String>> environments)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
        TargetDefinitionResolver resolver = new TargetDefinitionResolver(environments, agent, logger);
        content.add(resolver.resolveContent(definition));
    }

    // --------------------------------------------------------------------------------
    // creating copy&paste from org.eclipse.equinox.internal.p2.repository.Credentials.forLocation(URI, boolean,
    // AuthenticationInfo)
    public void setCredentials(URI location, String username, String password) {
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

        // if URI is not opaque, just getting the host may be enough
        String host = location.getHost();
        if (host == null) {
            String scheme = location.getScheme();
            if (URIUtil.isFileURI(location) || scheme == null) {
                // If the URI references a file, a password could possibly be needed for the directory
                // (it could be a protected zip file representing a compressed directory) - in this
                // case the key is the path without the last segment.
                // Using "Path" this way may result in an empty string - which later will result in
                // an invalid key.
                host = new Path(location.toString()).removeLastSegments(1).toString();
            } else {
                // it is an opaque URI - details are unknown - can only use entire string.
                host = location.toString();
            }
        }
        String nodeKey;
        try {
            nodeKey = URLEncoder.encode(host, "UTF-8"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e2) {
            // fall back to default platform encoding
            try {
                // Uses getProperty "file.encoding" instead of using deprecated URLEncoder.encode(String location)
                // which does the same, but throws NPE on missing property.
                String enc = System.getProperty("file.encoding");//$NON-NLS-1$
                if (enc == null) {
                    throw new UnsupportedEncodingException(
                            "No UTF-8 encoding and missing system property: file.encoding"); //$NON-NLS-1$
                }
                nodeKey = URLEncoder.encode(host, enc);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;

        ISecurePreferences prefNode = securePreferences.node(nodeName);

        try {
            if (!username.equals(prefNode.get(IRepository.PROP_USERNAME, username))
                    || !password.equals(prefNode.get(IRepository.PROP_PASSWORD, password))) {
                logger.info("Redefining access credentials for repository host " + host);
            }
            prefNode.put(IRepository.PROP_USERNAME, username, false);
            prefNode.put(IRepository.PROP_PASSWORD, password, false);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------

    private File projectLocation;

    // TODO 364134 get rid of this method
    public void setProjectLocation(File projectLocation) {
        this.projectLocation = projectLocation;
    }

    public TargetPlatform buildTargetPlatform() {
        // TODO 364134 get rid of this special handling of pomDependency artifacts: there should be one p2 artifact repo view on the target platform
        IArtifactRepository resolutionContextArtifactRepo = getSupplementaryArtifactRepository();
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey.forResolutionContextArtifacts(projectLocation);
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, resolutionContextArtifactRepo);
        logger.debug("Registered artifact repository " + blackboardKey);

        assertNoDuplicateReactorUIs();
        Collection<IInstallableUnit> allTargetPlatformIUs = gatherAvailableInstallableUnits(monitor);

        List<URI> allRemoteArtifactRepositories = new ArrayList<URI>();
        for (IArtifactRepository artifactRepository : artifactRepositories) {
            allRemoteArtifactRepositories.add(artifactRepository.getLocation());
        }
        for (TargetPlatformContent contentPart : content) {
            allRemoteArtifactRepositories.addAll(contentPart.getArtifactRepositoryLocations());
        }

        return new TargetPlatformImpl(allTargetPlatformIUs, mavenInstallableUnits, reactorProjectIUs,
                reactorProjectSecondaryIUs, localMetadataRepository, executionEnvironment,
                allRemoteArtifactRepositories, localArtifactRepository, agent, logger);
    }

    // -------------------------------------------------------------------------

    private Collection<IInstallableUnit> gatherAvailableInstallableUnits(IProgressMonitor monitor) {
        Collection<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for (TargetPlatformContent contentPart : content) {
            filterJREUIs(result, contentPart.getUnits());
        }

        filterJREUIs(result, mavenInstallableUnits.keySet());

        SubMonitor sub = SubMonitor.convert(monitor, metadataRepositories.size() * 200);
        for (IMetadataRepository repository : metadataRepositories) {
            IQueryResult<IInstallableUnit> matches = repository.query(QueryUtil.ALL_UNITS, sub.newChild(100));
            for (Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext();) {
                IInstallableUnit iu = it.next();

                if (isPartialIU(iu)) {
                    logger.debug("PARTIAL IU: " + iu);
                    continue;
                }

                if (isJREUI(iu)) {
                    continue;
                }

                if (!isReactorInstallableUnit(iu)) {
                    if (!reactorInstallableUnitIds.contains(iu.getId())) {
                        result.add(iu);
                    } else {
                        // this produces too much noise in STDOUT
//                        logger.debug( "External IU " + iu + " from repository " + repository.getLocation()
//                            + " has the same id as reactor project. External IU is ignored." );
                    }
                }
            }
        }
        result.addAll(getJREIUs());
        sub.done();
        // this is a real shame
        return result;
    }

    /**
     * p2 repositories are polluted with useless a.jre/config.a.jre IUs. These IUs do not represent
     * current/desired JRE and can expose resolver to packages that are not actually available.
     */
    private void filterJREUIs(Collection<IInstallableUnit> result, Collection<? extends IInstallableUnit> units) {
        for (IInstallableUnit iu : units) {
            if (isJREUI(iu)) {
                logger.debug("Ignored a.jre installable unit " + iu.toString());
                continue;
            }
            result.add(iu);
        }
    }

    private boolean isJREUI(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    private static boolean isPartialIU(IInstallableUnit iu) {
        return Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue();
    }

    private boolean isReactorInstallableUnit(IInstallableUnit iu) {
        return mavenInstallableUnits.get(iu) instanceof IReactorArtifactFacade;
    }

    /**
     * Return IUs that represent packages provided by target JRE
     */
    private Collection<IInstallableUnit> getJREIUs() {
        PublisherResult results = new PublisherResult();
        new JREAction(executionEnvironment).perform(new PublisherInfo(), results, new NullProgressMonitor());
        return results.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toUnmodifiableSet();
    }

    // -------------------------------------------------------------------------------

    private final IMetadataRepositoryManager metadataRepositoryManager;

    private final IArtifactRepositoryManager artifactRepositoryManager;

    private final P2RepositoryCache repositoryCache;

    // -------------------------------------------------------------------------------

    private void assertNoDuplicateReactorUIs() throws DuplicateReactorIUsException {
        Map<IInstallableUnit, Set<File>> reactorUIs = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<IInstallableUnit, Set<File>>();

        for (Map.Entry<ClassifiedLocation, Set<IInstallableUnit>> entry : reactorProjectIUs.entrySet()) {
            for (IInstallableUnit iu : entry.getValue()) {
                Set<File> locations = reactorUIs.get(iu);
                if (locations == null) {
                    locations = new LinkedHashSet<File>();
                    reactorUIs.put(iu, locations);
                }
                locations.add(entry.getKey().getLocation());
                if (locations.size() > 1) {
                    duplicateReactorUIs.put(iu, locations);
                }
            }
        }

        if (!duplicateReactorUIs.isEmpty()) {
            throw new DuplicateReactorIUsException(duplicateReactorUIs);
        }
    }

}
