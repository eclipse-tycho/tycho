/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - p2 data handling for POM dependencies (bug 342851/TYCHO-570)
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

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
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.maven.repository.MavenArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.MavenMetadataRepository;
import org.eclipse.tycho.p2.maven.repository.MavenMirrorRequest;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.p2.resolver.facade.P2RepositoryCache;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;

@SuppressWarnings("restriction")
public class P2ResolverImpl implements P2Resolver {
    // BundlesAction.CAPABILITY_NS_OSGI_BUNDLE
    private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle";

    private static final IArtifactRequest[] ARTIFACT_REQUEST_ARRAY = new IArtifactRequest[0];

    private final P2GeneratorImpl generator = new P2GeneratorImpl(true);

    private P2RepositoryCache repositoryCache;

    /**
     * All known P2 metadata repositories, including maven local repository
     */
    private final List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();

    /**
     * All known P2 artifact repositories, NOT including maven local repository.
     */
    private final List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

    /** maven local repository as P2 IArtifactRepository */
    private LocalArtifactRepository localRepository;

    /** maven local repository as P2 IMetadataRepository */
    private LocalMetadataRepository localMetadataRepository;

    /**
     * Maps maven artifact location (project basedir or local repo path) to installable units
     */
    // private final Map<File, Set<IInstallableUnit>> mavenArtifactIUs = new HashMap<File, Set<IInstallableUnit>>();

    /**
     * Maps maven artifact location (project basedir or local repo path) to project type
     */
    // private final Map<File, String> mavenArtifactTypes = new LinkedHashMap<File, String>();

    /**
     * Maps installable unit id to locations of reactor projects
     */
    // private final Map<String, Set<File>> iuReactorProjects = new HashMap<String, Set<File>>();

    private IProgressMonitor monitor = new NullProgressMonitor();

    private Map<ClassifiedLocation, IArtifactFacade> mavenArtifacts = new HashMap<ClassifiedLocation, IArtifactFacade>();

    private Map<ClassifiedLocation, Set<IInstallableUnit>> mavenLocations = new HashMap<ClassifiedLocation, Set<IInstallableUnit>>();

    private Map<IInstallableUnit, IArtifactFacade> mavenInstallableUnits = new HashMap<IInstallableUnit, IArtifactFacade>();

    private Set<String> reactorInstallableUnitIds = new HashSet<String>();

    /**
     * Target runtime environment properties
     */
    private List<Map<String, String>> environments;

    private final List<IRequirement> additionalRequirements = new ArrayList<IRequirement>();

    private MavenLogger logger;

    private boolean offline;

    private IProvisioningAgent agent;

    private File localRepositoryLocation;

    public P2ResolverImpl() {
    }

    public void addReactorArtifact(IReactorArtifactFacade artifact) {
        Set<IInstallableUnit> units = toSet(artifact.getDependencyMetadata(), IInstallableUnit.class);

        addMavenArtifact(artifact, units);

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

    public void addMavenArtifact(IArtifactFacade artifact) {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

        generator.generateMetadata(artifact, environments, units, null);

        addMavenArtifact(artifact, units);
    }

    public void addTychoArtifact(IArtifactFacade artifact, IArtifactFacade p2MetadataData) {
        try {
            addMavenArtifact(artifact, readUnits(p2MetadataData));
        } catch (IOException e) {
            throw new RuntimeException("failed to read p2 metadata", e);
        }
    }

    private Set<IInstallableUnit> readUnits(IArtifactFacade p2MetadataData) throws IOException {
        FileInputStream inputStream = new FileInputStream(p2MetadataData.getLocation());
        try {
            MetadataIO io = new MetadataIO();
            return io.readXML(inputStream);
        } finally {
            inputStream.close();
        }
    }

    void addMavenArtifact(IArtifactFacade artifact, Set<IInstallableUnit> units) {
        ClassifiedLocation key = new ClassifiedLocation(artifact);
        mavenArtifacts.put(key, artifact);
        mavenLocations.put(key, units);

        for (IInstallableUnit unit : units) {
            mavenInstallableUnits.put(unit, artifact);
            if (logger.isDebugEnabled()) {
                logger.debug("P2Resolver: artifact "
                        + new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()).toString()
                        + " resolves installable unit " + new VersionedId(unit.getId(), unit.getVersion()));
            }
        }
    }

    public void addP2Repository(URI location) {
        // check metadata cache, first
        IMetadataRepository metadataRepository = (IMetadataRepository) repositoryCache.getMetadataRepository(location);
        IArtifactRepository artifactRepository = (IArtifactRepository) repositoryCache.getArtifactRepository(location);
        if (metadataRepository != null && (offline || artifactRepository != null)) {
            // cache hit
            metadataRepositories.add(metadataRepository);
            if (artifactRepository != null) {
                artifactRepositories.add(artifactRepository);
            }
            logger.info("Adding repository (cached) " + location.toASCIIString());
            return;
        }

        if (agent == null) {
            if (localRepositoryLocation == null) {
                throw new IllegalStateException("Maven local repository location is null");
            }

            try {
                agent = Activator.newProvisioningAgent();

                TychoP2RepositoryCacheManager cacheMgr = new TychoP2RepositoryCacheManager();
                cacheMgr.setOffline(offline);
                cacheMgr.setLocalRepositoryLocation(localRepositoryLocation);

                agent.registerService(CacheManager.SERVICE_NAME, cacheMgr);
            } catch (ProvisionException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) agent
                    .getService(IMetadataRepositoryManager.SERVICE_NAME);
            if (metadataRepositoryManager == null) {
                throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
            }

            metadataRepository = metadataRepositoryManager.loadRepository(location, monitor);
            metadataRepositories.add(metadataRepository);

            if (!offline || URIUtil.isFileURI(location)) {
                IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) agent
                        .getService(IArtifactRepositoryManager.SERVICE_NAME);
                if (artifactRepositoryManager == null) {
                    throw new IllegalStateException("No artifact repository manager found"); //$NON-NLS-1$
                }

                artifactRepository = artifactRepositoryManager.loadRepository(location, monitor);
                artifactRepositories.add(artifactRepository);

                forceSingleThreadedDownload(artifactRepositoryManager, artifactRepository);
            }

            repositoryCache.putRepository(location, metadataRepository, artifactRepository);

            // processPartialIUs( metadataRepository, artifactRepository );
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void forceSingleThreadedDownload(IArtifactRepositoryManager artifactRepositoryManager,
            IArtifactRepository artifactRepository) {
        try {
            if (artifactRepository instanceof SimpleArtifactRepository) {
                forceSingleThreadedDownload((SimpleArtifactRepository) artifactRepository);
            } else if (artifactRepository instanceof CompositeArtifactRepository) {
                forceSingleThreadedDownload(artifactRepositoryManager, (CompositeArtifactRepository) artifactRepository);
            }
        } catch (Exception e) {
            // we've tried
        }
    }

    protected void forceSingleThreadedDownload(IArtifactRepositoryManager artifactRepositoryManager,
            CompositeArtifactRepository artifactRepository) throws ProvisionException {
        List<URI> children = artifactRepository.getChildren();
        for (URI child : children) {
            forceSingleThreadedDownload(artifactRepositoryManager,
                    artifactRepositoryManager.loadRepository(child, monitor));
        }
    }

    protected void forceSingleThreadedDownload(SimpleArtifactRepository artifactRepository) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = AbstractRepository.class.getDeclaredField("properties");
        field.setAccessible(true);
        OrderedProperties p = (OrderedProperties) field.get(artifactRepository);
        p.put(org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.PROP_MAX_THREADS, "1");
    }

    public List<P2ResolutionResult> resolveProject(File projectLocation) {
        ArrayList<P2ResolutionResult> results = new ArrayList<P2ResolutionResult>();

        for (Map<String, String> properties : environments) {
            results.add(resolveProject(projectLocation, new ProjectorResolutionStrategy(properties, logger)));
        }

        return results;
    }

    public P2ResolutionResult collectProjectDependencies(File projectLocation) {
        return resolveProject(projectLocation, new DependencyCollector(logger));
    }

    public P2ResolutionResult resolveMetadata(Map<String, String> properties) {
        ProjectorResolutionStrategy strategy = new ProjectorResolutionStrategy(properties, logger);
        strategy.setAvailableInstallableUnits(gatherAvailableInstallableUnits(monitor));
        strategy.setRootInstallableUnits(new HashSet<IInstallableUnit>());
        strategy.setAdditionalRequirements(additionalRequirements);

        P2ResolutionResult result = new P2ResolutionResult();
        for (IInstallableUnit iu : strategy.resolve(monitor)) {
            result.addArtifact(TYPE_INSTALLABLE_UNIT, iu.getId(), iu.getVersion().toString(), null, null, iu);
        }
        return result;
    }

    protected P2ResolutionResult resolveProject(File projectLocation, ResolutionStrategy strategy) {
        assertNoDuplicateReactorUIs();

        strategy.setAvailableInstallableUnits(gatherAvailableInstallableUnits(monitor));
        LinkedHashSet<IInstallableUnit> projectIUs = getProjectIUs(projectLocation);
        strategy.setRootInstallableUnits(projectIUs);
        strategy.setAdditionalRequirements(additionalRequirements);

        Collection<IInstallableUnit> newState = strategy.resolve(monitor);

        List<MavenMirrorRequest> requests = new ArrayList<MavenMirrorRequest>();
        for (IInstallableUnit iu : newState) {
            // maven IUs either come from reactor or local maven repository, no need to download them from p2 repos
            if (getMavenArtifact(iu) == null) {
                Collection<IArtifactKey> artifactKeys = iu.getArtifacts();
                for (IArtifactKey key : artifactKeys) {
                    requests.add(new MavenMirrorRequest(key, localRepository));
                }
            }
        }

        for (IArtifactRepository artifactRepository : artifactRepositories) {
            artifactRepository.getArtifacts(requests.toArray(ARTIFACT_REQUEST_ARRAY), monitor);

            requests = filterCompletedRequests(requests);
        }

        localRepository.save();
        localMetadataRepository.save();

        // check for locally installed artifacts, which are not available from any remote repo
        for (Iterator<MavenMirrorRequest> iter = requests.iterator(); iter.hasNext();) {
            MavenMirrorRequest request = iter.next();
            if (localRepository.contains(request.getArtifactKey())) {
                iter.remove();
            }
        }

        if (!requests.isEmpty()) {
            StringBuilder msg = new StringBuilder("Could not download artifacts from any repository\n");
            for (MavenMirrorRequest request : requests) {
                msg.append("   ").append(request.getArtifactKey().toExternalForm()).append('\n');
            }

            throw new RuntimeException(msg.toString());
        }

        return toResolutionResult(newState);
    }

    private boolean isReactorInstallableUnit(IInstallableUnit iu) {
        return mavenInstallableUnits.get(iu) instanceof IReactorArtifactFacade;
    }

    private void assertNoDuplicateReactorUIs() throws DuplicateReactorIUsException {
        Map<IInstallableUnit, Set<File>> reactorUIs = new HashMap<IInstallableUnit, Set<File>>();
        Map<IInstallableUnit, Set<File>> duplicateReactorUIs = new HashMap<IInstallableUnit, Set<File>>();

        for (Map.Entry<ClassifiedLocation, Set<IInstallableUnit>> entry : mavenLocations.entrySet()) {
            if (mavenArtifacts.get(entry.getKey()) instanceof IReactorArtifactFacade) {
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
        }

        if (!duplicateReactorUIs.isEmpty()) {
            throw new DuplicateReactorIUsException(duplicateReactorUIs);
        }
    }

    private P2ResolutionResult toResolutionResult(Collection<IInstallableUnit> newState) {
        P2ResolutionResult result = new P2ResolutionResult();
        for (IInstallableUnit iu : newState) {
            IArtifactFacade mavenArtifact = getMavenArtifact(iu);
            if (mavenArtifact != null) {
                addMavenArtifact(result, mavenArtifact, iu);
            } else {
                for (IArtifactKey key : iu.getArtifacts()) {
                    addArtifactFile(result, iu, key);
                }
            }
        }

        collectNonReactorIUs(result, newState);
        return result;
    }

    private void collectNonReactorIUs(P2ResolutionResult result, Collection<IInstallableUnit> newState) {
        for (IInstallableUnit iu : newState) {
            if (!isReactorArtifact(iu)) {
                result.addNonReactorUnit(iu);
            }
        }
    }

    private boolean isReactorArtifact(IInstallableUnit iu) {
        return getMavenArtifact(iu) instanceof IReactorArtifactFacade;
    }

    private LinkedHashSet<IInstallableUnit> getProjectIUs(File location) {
        LinkedHashSet<IInstallableUnit> ius = new LinkedHashSet<IInstallableUnit>();

        for (Map.Entry<ClassifiedLocation, Set<IInstallableUnit>> entry : mavenLocations.entrySet()) {
            if (location.equals(entry.getKey().getLocation())) {
                ius.addAll(entry.getValue());
            }
        }

        return ius;
    }

    private void addArtifactFile(P2ResolutionResult platform, IInstallableUnit iu, IArtifactKey key) {
        File file = getLocalArtifactFile(key);
        if (file == null) {
            return;
        }

        IArtifactFacade reactorArtifact = getMavenArtifact(iu);

        String id = iu.getId();
        String version = iu.getVersion().toString();
        String mavenClassidier = reactorArtifact != null ? reactorArtifact.getClassidier() : null;

        if (PublisherHelper.OSGI_BUNDLE_CLASSIFIER.equals(key.getClassifier())) {
            platform.addArtifact(P2Resolver.TYPE_ECLIPSE_PLUGIN, id, version, file, mavenClassidier, iu);
        } else if (PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER.equals(key.getClassifier())) {
            String featureId = getFeatureId(iu);
            if (featureId != null) {
                platform.addArtifact(P2Resolver.TYPE_ECLIPSE_FEATURE, featureId, version, file, mavenClassidier, iu);
            }
        }

        // ignore other/unknown artifacts, like binary blobs for now.
        // throw new IllegalArgumentException();
    }

    private IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        return mavenInstallableUnits.get(iu);
    }

    private void addMavenArtifact(P2ResolutionResult platform, IArtifactFacade mavenArtifact, IInstallableUnit iu) {
        String type = mavenArtifact.getPackagingType();
        String id = iu.getId();
        String version = iu.getVersion().toString();
        File location = mavenArtifact.getLocation();
        String mavenClassidier = mavenArtifact.getClassidier();

        if (TYPE_ECLIPSE_FEATURE.equals(type)) {
            id = getFeatureId(iu);
            if (id == null) {
                throw new IllegalStateException("Feature id is null for maven artifact at "
                        + mavenArtifact.getLocation() + " with classifier " + mavenArtifact.getClassidier());
            }
        } else if ("jar".equals(type)) {
            // this must be an OSGi bundle coming from a maven repository
            // TODO check if iu actually provides CAPABILITY_NS_OSGI_BUNDLE capability
            type = TYPE_ECLIPSE_PLUGIN;
        }

        platform.addArtifact(type, id, version, location, mavenClassidier, iu);
    }

    private String getFeatureId(IInstallableUnit iu) {
        for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
            if (PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE.equals(provided.getNamespace())) {
                return provided.getName();
            }
        }
        return null;
    }

    private File getLocalArtifactFile(IArtifactKey key) {
        for (IArtifactDescriptor descriptor : localRepository.getArtifactDescriptors(key)) {
            URI uri = localRepository.getLocation(descriptor);
            if (uri != null) {
                return new File(uri);
            }
        }

        return null;
    }

    private List<MavenMirrorRequest> filterCompletedRequests(List<MavenMirrorRequest> requests) {
        ArrayList<MavenMirrorRequest> filteredRequests = new ArrayList<MavenMirrorRequest>();
        for (MavenMirrorRequest request : requests) {
            if (request.getResult() == null || !request.getResult().isOK()) {
                filteredRequests.add(request);
            }
        }
        return filteredRequests;
    }

    public IQueryable<IInstallableUnit> gatherAvailableInstallableUnits(IProgressMonitor monitor) {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        result.addAll(mavenInstallableUnits.keySet());

        SubMonitor sub = SubMonitor.convert(monitor, metadataRepositories.size() * 200);
        for (IMetadataRepository repository : metadataRepositories) {
            IQueryResult<IInstallableUnit> matches = repository.query(QueryUtil.ALL_UNITS, sub.newChild(100));
            for (Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext();) {
                IInstallableUnit iu = it.next();

                if (isPartialIU(iu)) {
                    logger.debug("PARTIAL IU: " + iu);
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
        result.addAll(createJREIUs());
        sub.done();
        // this is a real shame
        return new QueryableArray(result.toArray(new IInstallableUnit[result.size()]));
    }

    private static boolean isPartialIU(IInstallableUnit iu) {
        return Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue();
    }

    /**
     * these dummy IUs are needed to satisfy Import-Package requirements to packages provided by the
     * JDK.
     */
    private Collection<IInstallableUnit> createJREIUs() {
        PublisherResult results = new PublisherResult();
        // TODO use the appropriate profile name
        new JREAction((String) null).perform(new PublisherInfo(), results, new NullProgressMonitor());
        return results.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet();
    }

    public void setLocalRepositoryLocation(File location) {
        this.localRepositoryLocation = location;
        URI uri = location.toURI();

        localRepository = (LocalArtifactRepository) repositoryCache.getArtifactRepository(uri);
        localMetadataRepository = (LocalMetadataRepository) repositoryCache.getMetadataRepository(uri);

        if (localRepository == null || localMetadataRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(location);
            LocalTychoRepositoryIndex artifactsIndex = new LocalTychoRepositoryIndex(location,
                    LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
            LocalTychoRepositoryIndex metadataIndex = new LocalTychoRepositoryIndex(location,
                    LocalTychoRepositoryIndex.METADATA_INDEX_RELPATH);

            localRepository = new LocalArtifactRepository(location, artifactsIndex, contentLocator);
            localMetadataRepository = new LocalMetadataRepository(uri, metadataIndex, contentLocator);

            repositoryCache.putRepository(uri, localMetadataRepository, localRepository);
        }

        // XXX remove old
        metadataRepositories.add(localMetadataRepository);
    }

    public void setEnvironments(List<Map<String, String>> environments) {
        this.environments = environments;
    }

    public void addDependency(String type, String id, String versionRange) {
        if (P2Resolver.TYPE_INSTALLABLE_UNIT.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id,
                    new VersionRange(versionRange), null, false, true));
        } else if (P2Resolver.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, id,
                    new VersionRange(versionRange), null, false, true));
        }
    }

    public void addMavenRepository(URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator) {
        MavenMetadataRepository metadataRepository = (MavenMetadataRepository) repositoryCache
                .getMetadataRepository(location);
        MavenArtifactRepository artifactRepository = (MavenArtifactRepository) repositoryCache
                .getArtifactRepository(location);

        if (metadataRepository == null || artifactRepository == null) {
            metadataRepository = new MavenMetadataRepository(location, projectIndex, contentLocator);
            artifactRepository = new MavenArtifactRepository(location, projectIndex, contentLocator);

            repositoryCache.putRepository(location, metadataRepository, artifactRepository);
        }

        metadataRepositories.add(metadataRepository);
        artifactRepositories.add(artifactRepository);
    }

    public void setLogger(MavenLogger logger) {
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor(logger);
    }

    public void setRepositoryCache(P2RepositoryCache repositoryCache) {
        this.repositoryCache = repositoryCache;
    }

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

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public void stop() {
        if (agent != null) {
            agent.stop();
        }
    }

    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

}
