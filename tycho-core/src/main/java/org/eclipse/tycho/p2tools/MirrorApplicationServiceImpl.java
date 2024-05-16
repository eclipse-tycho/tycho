/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Bachmann electronic GmbH. - Support for ignoreError flag
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;
import org.eclipse.equinox.p2.internal.repository.mirroring.Mirroring;
import org.eclipse.equinox.p2.internal.repository.tools.Activator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.internal.repository.tools.XZCompressor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.IUDescription;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorOptions;
import org.eclipse.tycho.p2tools.copiedfromp2.RecreateRepositoryApplication;
import org.eclipse.tycho.p2tools.copiedfromp2.RepositoryDescriptor;

@Component(role = MirrorApplicationService.class)
public class MirrorApplicationServiceImpl implements MirrorApplicationService {

    private static final String P2_INDEX_FILE = "p2.index";

    private static final String MIRROR_FAILURE_MESSAGE = "Mirroring failed";

    @Requirement
    Logger logger;

    @Requirement
    IProvisioningAgent agent;

    @Override
    public void mirrorStandalone(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<IUDescription> seedIUs, MirrorOptions mirrorOptions, BuildDirectory tempDirectory)
            throws FacadeException {
        agent.getService(IArtifactRepositoryManager.class); //force init of framework if not already done!
        final TychoMirrorApplication mirrorApp = createMirrorApplication(sources, destination, agent, logger);
        mirrorApp.setSlicingOptions(createSlicingOptions(mirrorOptions));
        mirrorApp.setIgnoreErrors(mirrorOptions.isIgnoreErrors());
        try {
            // we want to see mirror progress as this is a possibly long-running operation
            mirrorApp.setVerbose(true);
            mirrorApp.setLog(new LogListener(logger));
            mirrorApp.setSourceIUs(querySourceIus(seedIUs, mirrorApp.getCompositeMetadataRepository(), sources));
            IStatus returnStatus = mirrorApp.run(null);
            checkStatus(returnStatus, mirrorOptions.isIgnoreErrors());

        } catch (ProvisionException e) {
            throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(e.getStatus()), e);
        }
    }

    public void setAgent(IProvisioningAgent agent) {
        this.agent = agent;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private static SlicingOptions createSlicingOptions(MirrorOptions mirrorOptions) {
        SlicingOptions slicingOptions = new SlicingOptions();
        slicingOptions.considerStrictDependencyOnly(mirrorOptions.isFollowStrictOnly());
        slicingOptions.everythingGreedy(mirrorOptions.isIncludeNonGreedy());
        slicingOptions.followOnlyFilteredRequirements(mirrorOptions.isFollowOnlyFilteredRequirements());
        slicingOptions.includeOptionalDependencies(mirrorOptions.isIncludeOptional());
        slicingOptions.latestVersionOnly(mirrorOptions.isLatestVersionOnly());
        slicingOptions.setFilter(mirrorOptions.getFilter());
        return slicingOptions;
    }

    private static List<IInstallableUnit> querySourceIus(Collection<IUDescription> sourceIUs,
            IMetadataRepository repository, RepositoryReferences sources) throws FacadeException {
        if (sourceIUs == null || sourceIUs.isEmpty()) {
            return null;
        }
        List<IInstallableUnit> result = new ArrayList<>();
        for (IUDescription iu : sourceIUs) {
            IQuery<IInstallableUnit> iuQuery = createQuery(iu);
            Iterator<IInstallableUnit> queryResult = repository.query(iuQuery, null).iterator();
            if (!queryResult.hasNext()) {
                throw new FacadeException("Could not find IU " + iu.toString() + " in any of the source repositories "
                        + sources.getMetadataRepositories(), null);
            }
            while (queryResult.hasNext()) {
                result.add(queryResult.next());
            }
        }
        return result;
    }

    private static IQuery<IInstallableUnit> createQuery(IUDescription iu) {
        String id = iu.getId();
        String version = iu.getVersion();
        if (iu.getQueryMatchExpression() != null) {
            return QueryUtil.createMatchQuery(iu.getQueryMatchExpression(), (Object[]) iu.getQueryParameters());
        } else {
            if (version == null || version.isEmpty()) {
                return QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id));
            } else {
                return QueryUtil.createIUQuery(id, Version.parseVersion(version));
            }
        }
    }

    @Override
    public void mirrorReactor(RepositoryReferences sources, DestinationRepositoryDescriptor destination,
            Collection<DependencySeed> projectSeeds, BuildContext context, boolean includeAllDependencies,
            boolean includeAllSource, boolean includeRequiredBundles, boolean includeRequiredFeatures,
            boolean filterProvided, boolean addOnlyProvidingRepoReferences, Map<String, String> filterProperties)
            throws FacadeException {
        final TychoMirrorApplication mirrorApp = createMirrorApplication(sources, destination, agent, logger);

        // mirror scope: seed units...
        try {
            mirrorApp.setSourceIUs(
                    toInstallableUnitList(projectSeeds, mirrorApp.getCompositeMetadataRepository(), sources));
            mirrorApp.setIncludeSources(includeAllSource, sources.getTargetPlatform());
            mirrorApp.setIncludeRequiredBundles(includeRequiredBundles);
            mirrorApp.setIncludeRequiredFeatures(includeRequiredFeatures);
            mirrorApp.setFilterProvided(filterProvided);
            mirrorApp.setAddOnlyProvidingRepoReferences(addOnlyProvidingRepoReferences);
            mirrorApp.setEnvironments(context.getEnvironments());
            SlicingOptions options = new SlicingOptions();
            options.considerStrictDependencyOnly(!includeAllDependencies);
            Map<String, String> filter = options.getFilter();
            addFilterForFeatureJARs(filter);
            if (filterProperties != null) {
                filter.putAll(filterProperties);
            }
            mirrorApp.setSlicingOptions(options);
            LogListener logListener = new LogListener(logger);
            mirrorApp.setLog(logListener);

            IStatus returnStatus = mirrorApp.run(null);
            checkStatus(returnStatus, false);
            logListener.showHelpForLoggedMessages();
        } catch (ProvisionException e) {
            throw new FacadeException(MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(e.getStatus()), e);
        }
        recreateArtifactRepository(destination);
    }

    private void xzCompress(DestinationRepositoryDescriptor destination) throws FacadeException {
        if (!destination.isXZCompress()) {
            return;
        }
        try {
            XZCompressor xzCompressor = new XZCompressor();
            xzCompressor.setPreserveOriginalFile(destination.shouldKeepNonXzIndexFiles());
            xzCompressor.setRepoFolder(destination.getLocation().getAbsolutePath());
            xzCompressor.compressRepo();
        } catch (IOException e) {
            throw new FacadeException("XZ compression failed", e);
        }
    }

    @Override
    public void recreateArtifactRepository(DestinationRepositoryDescriptor destination) throws FacadeException {
        // bug 357513 - force artifact repo recreation which will
        // create the missing md5 checksums
        if (destination.isMetaDataOnly()) {
            return;
        }
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.setAppend(true);
        descriptor.setFormat(null);
        descriptor.setKind("artifact"); //$NON-NLS-1$
        File location = destination.getLocation();
        File artifactsXz = new File(location, "artifacts.xml.xz");
        if (artifactsXz.exists()) {
            artifactsXz.delete();
        }
        descriptor.setLocation(location.toURI());
        RecreateRepositoryApplication application = new RecreateRepositoryApplication(agent);
        application.setArtifactRepository(descriptor.getRepoLocation());
        try {
            application.run(new NullProgressMonitor());
        } catch (ProvisionException e) {
            throw new FacadeException("Recreate artifact repository failed", e);
        }
        xzCompress(destination);
    }

    private static TychoMirrorApplication createMirrorApplication(RepositoryReferences sources,
            DestinationRepositoryDescriptor destination, IProvisioningAgent agent, Logger logger) {
        final TychoMirrorApplication mirrorApp = new TychoMirrorApplication(agent, destination, logger);
        mirrorApp.setRaw(false);

        List<RepositoryDescriptor> sourceDescriptors = createSourceDescriptors(sources);
        for (RepositoryDescriptor sourceDescriptor : sourceDescriptors) {
            mirrorApp.addSource(sourceDescriptor);
        }
        mirrorApp.addDestination(createDestinationDescriptor(destination));

        // mirrorApp.setValidate( true ); // TODO Broken; fix at Eclipse

        return mirrorApp;
    }

    private static RepositoryDescriptor createDestinationDescriptor(DestinationRepositoryDescriptor destination) {
        final RepositoryDescriptor destinationDescriptor = new RepositoryDescriptor();
        destinationDescriptor.setLocation(destination.getLocation().toURI());
        destinationDescriptor.setAppend(destination.isAppend());
        destinationDescriptor.setName(destination.getName());
        destinationDescriptor.setCompressed(destination.isCompress());
        if (destination.isMetaDataOnly()) {
            // only mirror metadata
            destinationDescriptor.setKind(RepositoryDescriptor.KIND_METADATA);
        } else {
            // metadata and artifacts is the default
        }
        return destinationDescriptor;
    }

    /**
     * Set filter value so that the feature JAR units and artifacts are included when mirroring.
     */
    private static void addFilterForFeatureJARs(Map<String, String> filter) {
        filter.put("org.eclipse.update.install.features", "true");
    }

    private static List<RepositoryDescriptor> createSourceDescriptors(RepositoryReferences sources) {
        List<RepositoryDescriptor> result = new ArrayList<>();
        createSourceRepositories(result, sources.getMetadataRepositories(), RepositoryDescriptor.KIND_METADATA);
        createSourceRepositories(result, sources.getArtifactRepositories(), RepositoryDescriptor.KIND_ARTIFACT);
        return result;
    }

    private static void createSourceRepositories(List<RepositoryDescriptor> result, Collection<URI> repositoryLocations,
            String repositoryKind) {
        for (URI repositoryLocation : repositoryLocations) {
            RepositoryDescriptor repository = new RepositoryDescriptor();
            repository.setKind(repositoryKind);
            repository.setLocation(repositoryLocation);
            result.add(repository);
        }
    }

    private static List<IInstallableUnit> toInstallableUnitList(Collection<DependencySeed> seeds,
            IMetadataRepository sourceRepository, RepositoryReferences sourceRepositoryNames) throws FacadeException {
        List<IInstallableUnit> result = new ArrayList<>(seeds.size());

        for (DependencySeed seed : seeds) {
            if (seed.getInstallableUnit() == null) {
                // TODO 372780 drop this when getInstallableUnit can no longer be null
                String unitId = seed.getId()
                        + (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(seed.getType()) ? ".feature.group" : "");
                result.addAll(querySourceIus(Collections.singletonList(new IUDescription(unitId, null)),
                        sourceRepository, sourceRepositoryNames));
            } else {
                result.add(seed.getInstallableUnit());
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("List of seed units for repository aggregation must not be empty");
        }
        return result;
    }

    private void checkStatus(IStatus status, boolean ignoreErrors) throws FacadeException {
        if (status.matches(IStatus.ERROR)) {
            String message = MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems(status);
            if (!ignoreErrors) {
                throw new FacadeException(message, StatusTool.findException(status));
            }
            logger.info(message);
        }
    }

    static class LogListener implements IArtifactMirrorLog {
        private static final String MIRROR_TOOL_MESSAGE_PREFIX = "Mirror tool: ";
        private static final URI MIRROR_TOOL_MESSAGE_HELP = URI
                .create("https://wiki.eclipse.org/Tycho_Messages_Explained#Mirror_tool");

        private final Logger logger;
        private boolean hasLogged = false;

        LogListener(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void log(IArtifactDescriptor descriptor, IStatus status) {
            if (!status.isOK()) {
                logger.debug(MIRROR_TOOL_MESSAGE_PREFIX + StatusTool.toLogMessage(status));
                hasLogged = true;
            }
        }

        @Override
        public void log(IStatus status) {
            if (!status.isOK()) {
                logger.warn(MIRROR_TOOL_MESSAGE_PREFIX + StatusTool.toLogMessage(status));
                hasLogged = true;
            }
        }

        public void showHelpForLoggedMessages() {
            if (hasLogged) {
                logger.warn("More information on the preceding warning(s) can be found here:");
                logger.warn("- " + MIRROR_TOOL_MESSAGE_HELP);
            }

        }

        @Override
        public void close() {
        }

    }

    private static final class MappingRule {
        public final String filter;
        public final String urlPattern;

        public MappingRule(String filter, String urlPattern) {
            this.filter = filter;
            this.urlPattern = urlPattern;
        }
    }

    @Override
    public void addMavenMappingRules(File repository, URI[] mavenRepositories) throws FacadeException {
        SimpleArtifactRepositoryFactory repoFactory = new SimpleArtifactRepositoryFactory();
        SimpleArtifactRepository repo = null;
        try {
            repo = (SimpleArtifactRepository) repoFactory.load(repository.toURI(),
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
        } catch (ProvisionException ex) {
            throw new FacadeException(ex);
        }
        if (repo == null) {
            throw new IllegalStateException("Repository couldn't be loaded");
        }
        LinkedList<MappingRule> rules = new LinkedList<>();
        for (String[] rule : repo.getRules()) {
            rules.add(new MappingRule(rule[0], rule[1]));
        }
        for (IArtifactDescriptor artifact : repo.getDescriptors()) {
            GAV gav = RepositoryLayoutHelper.getGAV(artifact.getProperties());
            String mavenClassifier = RepositoryLayoutHelper.getClassifier(artifact.getProperties());
            String mavenExtension = RepositoryLayoutHelper.getType(artifact.getProperties());
            // should care about classifier, extension and other
            if (gav != null && !gav.getVersion().endsWith("-SNAPSHOT")) {
                for (URI mavenRepo : mavenRepositories) {
                    IArtifactKey artifactKey = artifact.getArtifactKey();
                    URI mavenArtifactURI = URI.create(mavenRepo.toString() + '/'
                            + RepositoryLayoutHelper.getRelativePath(gav, mavenClassifier, mavenExtension));
                    try {
                        URLConnection connection = mavenArtifactURI.toURL().openConnection();
                        if (connection instanceof HttpURLConnection httpConnection) {
                            int responseCode = httpConnection.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                logger.debug(artifactKey.toString() + '/' + gav + " not found in " + mavenRepo);
                                continue;
                            }
                        }
                        // so far so good, continue
                        rules.addFirst(new MappingRule(
                                "(& (classifier=" + artifactKey.getClassifier() + ")(id=" + artifactKey.getId()
                                        + ")(version=" + artifactKey.getVersion().toString() + "))",
                                mavenArtifactURI.toString()));
                        File artifactFile = repo.getArtifactFile(artifactKey);
                        if (artifactFile != null) {
                            artifactFile.delete();
                        }
                        logger.info(artifactKey + " remapped to " + mavenArtifactURI);
                        break;
                    } catch (IOException ex) {
                        throw new FacadeException(ex);
                    }
                }
            }
        }
        String[][] newRules = new String[rules.size()][2];
        int i = 0;
        for (MappingRule rule : rules) {
            newRules[i][0] = rule.filter;
            newRules[i][1] = rule.urlPattern;
            i++;
        }
        repo.setRules(newRules);
        repo.save();
        DestinationRepositoryDescriptor desc = new DestinationRepositoryDescriptor(repository, repo.getName(),
                new File(repository, "artifacts.xml.xz").exists(), new File(repository, "artifacts.xml.xz").exists(),
                true, false, false);
        xzCompress(desc);
    }

    @Override
    public void mirrorDirect(IArtifactRepository sourceArtifactRepository,
            IQueryable<IInstallableUnit> sourceMetadataRepository, File repositoryDestination, String repositoryName)
            throws FacadeException {
        if (repositoryDestination.exists()) {
            FileUtils.deleteQuietly(repositoryDestination);
        }
        //See https://github.com/eclipse-equinox/p2/pull/418
        Objects.requireNonNull(sourceArtifactRepository.getProvisioningAgent(),
                "Source repository needs to have an agent");
        SimpleMetadataRepositoryFactory metadataRepositoryFactory = new SimpleMetadataRepositoryFactory();
        metadataRepositoryFactory.setAgent(agent);
        SimpleArtifactRepositoryFactory artifactRepositoryFactory = new SimpleArtifactRepositoryFactory();
        artifactRepositoryFactory.setAgent(agent);
        IArtifactRepository destinationArtifactRepository = artifactRepositoryFactory
                .create(repositoryDestination.toURI(), repositoryName, null, Map.of());
        IMetadataRepository destinationMetadataRepository = metadataRepositoryFactory
                .create(repositoryDestination.toURI(), repositoryName, null, Map.of());
        MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_mirroringStatus, null);
        Set<IArtifactKey> toMirror = new TreeSet<>(Comparator.comparing(IArtifactKey::getId).thenComparing(
                Comparator.comparing(IArtifactKey::getVersion).thenComparing(IArtifactKey::getClassifier)));
        multiStatus.add(destinationMetadataRepository.executeBatch(monitor -> {
            IQueryResult<IInstallableUnit> result = sourceMetadataRepository.query(QueryUtil.ALL_UNITS, monitor);
            Set<IInstallableUnit> units = result.toUnmodifiableSet();
            destinationMetadataRepository.addInstallableUnits(units);
            for (IInstallableUnit unit : units) {
                toMirror.addAll(unit.getArtifacts());
            }
        }, null));
        multiStatus.add(destinationArtifactRepository.executeBatch(monitor -> {
            Mirroring mirroring = new Mirroring(sourceArtifactRepository, destinationArtifactRepository, true);
            mirroring.setCompare(false);
            mirroring.setValidate(false);
            mirroring.setTransport(agent.getService(Transport.class));
            IArtifactKey[] keys = toMirror.toArray(IArtifactKey[]::new);
            mirroring.setArtifactKeys(keys);
            multiStatus.addAll(mirroring.run(false, false));
        }, null));
        if (!multiStatus.isOK()) {
            String logMessage = StatusTool.toLogMessage(multiStatus);
            if (multiStatus.getSeverity() == IStatus.INFO) {
                logger.info(logMessage, StatusTool.findException(multiStatus));
            } else if (multiStatus.getSeverity() == IStatus.WARNING) {
                logger.warn(logMessage, StatusTool.findException(multiStatus));
            }
            throw new FacadeException(logMessage, new CoreException(multiStatus));
        }
        writeP2Index(repositoryDestination);
        compressXml(repositoryDestination, "artifacts");
        compressXml(repositoryDestination, "content");
        try {
            XZCompressor xzCompressor = new XZCompressor();
            xzCompressor.setPreserveOriginalFile(true);
            xzCompressor.setRepoFolder(repositoryDestination.getAbsolutePath());
            xzCompressor.compressRepo();
        } catch (IOException e) {
            throw new FacadeException("XZ compression failed", e);
        }
    }

    private void writeP2Index(File repositoryDestination) throws FacadeException {
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("artifact.repository.factory.order", "artifacts.xml,!");
        properties.setProperty("metadata.repository.factory.order", "content.xml,!");
        try (FileOutputStream stream = new FileOutputStream(new File(repositoryDestination, P2_INDEX_FILE))) {
            properties.store(stream, null);
        } catch (IOException e) {
            throw new FacadeException("writing index file failed", e);
        }
    }

    private void compressXml(File repositoryDestination, String name) throws FacadeException {
        File jarFile = new File(repositoryDestination, name + ".jar");
        File xmlFile = new File(repositoryDestination, name + ".xml");
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile))) {
            jarOutputStream.putNextEntry(new JarEntry(xmlFile.getName()));
            Files.copy(xmlFile.toPath(), jarOutputStream);
        } catch (IOException e) {
            throw new FacadeException("compression failed", e);
        }
    }
}
