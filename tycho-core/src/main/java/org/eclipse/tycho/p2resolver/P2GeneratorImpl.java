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
 *    Christoph Läubrich - fix handling of optional secondary metadata
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.publisher.FeatureRootfileArtifactRepository;
import org.eclipse.tycho.core.publisher.TychoMavenPropertiesAdvice;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;
import org.eclipse.tycho.p2.publisher.AbstractMetadataGenerator;
import org.eclipse.tycho.p2.publisher.AuthoredIUAction;
import org.eclipse.tycho.p2.publisher.BundleDependenciesAction;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.publisher.DownloadStatsAdvice;
import org.eclipse.tycho.p2.publisher.FeatureDependenciesAction;
import org.eclipse.tycho.p2.publisher.P2Artifact;
import org.eclipse.tycho.p2.publisher.TransientArtifactRepository;
import org.eclipse.tycho.p2.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.repository.ArtifactsIO;
import org.eclipse.tycho.p2.repository.MetadataIO;
import org.eclipse.tycho.p2maven.actions.CategoryDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductDependenciesAction;
import org.eclipse.tycho.p2maven.actions.ProductFile2;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class P2GeneratorImpl extends AbstractMetadataGenerator implements P2Generator {
    private static final String[] SUPPORTED_TYPES = { PackagingType.TYPE_ECLIPSE_PLUGIN,
            PackagingType.TYPE_ECLIPSE_TEST_PLUGIN, PackagingType.TYPE_ECLIPSE_FEATURE,
            PackagingType.TYPE_ECLIPSE_REPOSITORY };

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Whether we need full p2 metadata (false) or just required capabilities.
     */
    private final boolean dependenciesOnly;
    private final MavenContext mavenContext;
    private final BuildPropertiesParser buildPropertiesParser;
    private final BundleReader bundleReader;

    @Inject
    public P2GeneratorImpl(MavenContext mavenContext, BuildPropertiesParser buildPropertiesParser, BundleReader bundleReader) {
        this(false, mavenContext, buildPropertiesParser, bundleReader);
    }

    protected P2GeneratorImpl(boolean dependenciesOnly, MavenContext mavenContext, BuildPropertiesParser buildPropertiesParser, BundleReader bundleReader) {
        this.dependenciesOnly = dependenciesOnly;
        this.mavenContext = mavenContext;
        this.buildPropertiesParser = buildPropertiesParser;
        this.bundleReader = bundleReader;
    }

    @Override
    public Map<String, IP2Artifact> generateMetadata(List<IArtifactFacade> artifacts, PublisherOptions options,
            final File targetDir) throws IOException {
        Map<String, IP2Artifact> result = new LinkedHashMap<>();

        for (IArtifactFacade artifact : artifacts) {

            PublisherInfo publisherInfo = new PublisherInfo();

            DependencyMetadata metadata;

            // meta data handling for root files
            if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(artifact.getPackagingType())) {
                int base = IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH;
                if (!options.isGenerateChecksums()) {
                    base = base | IPublisherInfo.A_NO_MD5;
                }
                publisherInfo.setArtifactOptions(base);
                FeatureRootfileArtifactRepository artifactsRepository = new FeatureRootfileArtifactRepository(
                        publisherInfo, targetDir);
                publisherInfo.setArtifactRepository(artifactsRepository);

                metadata = super.generateMetadata(artifact, null, publisherInfo, null, options);

                result.putAll(artifactsRepository.getPublishedArtifacts());
            } else if (PackagingType.TYPE_P2_IU.equals(artifact.getPackagingType())) {
                TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();
                publisherInfo.setArtifactRepository(artifactsRepository);
                final IArtifactFacade currentArtifact = artifact;
                IArtifactFacade targetDirAsArtifact = new IArtifactFacade() {
                    @Override
                    public String getVersion() {
                        return currentArtifact.getVersion();
                    }

                    @Override
                    public String getPackagingType() {
                        return currentArtifact.getPackagingType();
                    }

                    @Override
                    public File getLocation() {
                        return targetDir;
                    }

                    @Override
                    public String getGroupId() {
                        return currentArtifact.getGroupId();
                    }

                    @Override
                    public String getClassifier() {
                        return currentArtifact.getClassifier();
                    }

                    @Override
                    public String getArtifactId() {
                        return currentArtifact.getArtifactId();
                    }
                };
                metadata = super.generateMetadata(targetDirAsArtifact, null, publisherInfo, null, options);
            } else {
                int base = IPublisherInfo.A_PUBLISH;
                if (!options.isGenerateChecksums()) {
                    base = base | IPublisherInfo.A_NO_MD5;
                }
                publisherInfo.setArtifactOptions(base);
                TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();
                publisherInfo.setArtifactRepository(artifactsRepository);
                metadata = super.generateMetadata(artifact, null, publisherInfo, null, options);
            }

            // secondary metadata is meant to represent installable units that are provided by this project
            // but do not affect dependencies of the project itself. generateMetadata is called at the end
            // of project build lifecycle, and primary/secondary metadata separation is irrelevant at this point

            String classifier = artifact.getClassifier();
            getCanonicalArtifact(classifier, metadata.getArtifactDescriptors()).ifPresentOrElse(canonical -> {
                P2Artifact p2artifact = new P2Artifact(artifact.getLocation(), metadata.getInstallableUnits(),
                        canonical);
                result.put(classifier, p2artifact);
            }, () -> {
                logger.debug("Skip generation of secondary metadata for artifact = " + artifact
                        + ", as it does not have a canonical ArtifactDescriptor");
            });

        }

        return result;
    }

    private Optional<IArtifactDescriptor> getCanonicalArtifact(String classifier,
            Set<IArtifactDescriptor> artifactDescriptors) {
        for (IArtifactDescriptor descriptor : artifactDescriptors) {
            String _classifier = descriptor.getProperty(TychoConstants.PROP_CLASSIFIER);
            if (eq(classifier, _classifier) && descriptor.getProperty(IArtifactDescriptor.FORMAT) == null) {
                return Optional.of(descriptor);
            }
        }
        return Optional.empty();
    }

    private static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

    @Override
    public void persistMetadata(Map<String, IP2Artifact> metadata, File unitsXml, File artifactsXml)
            throws IOException {
        Set<IInstallableUnit> units = new LinkedHashSet<>();
        Set<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<>();
        for (IP2Artifact artifact : metadata.values()) {
            units.addAll(artifact.getInstallableUnits());
            artifactDescriptors.add(artifact.getArtifactDescriptor());
        }
        new MetadataIO().writeXML(units, unitsXml);
        new ArtifactsIO().writeXML(artifactDescriptors, artifactsXml);
    }

    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            PublisherOptions options) {
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
        publisherInfo.setArtifactRepository(new TransientArtifactRepository());

        return super.generateMetadata(artifact, environments, publisherInfo, null, options);
    }

    @Override
    protected List<IPublisherAction> getPublisherActions(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction) {

        if (!dependenciesOnly && optionalAction != null) {
            throw new IllegalArgumentException();
        }

        String packaging = artifact.getPackagingType();
        File location = artifact.getLocation();
        List<IPublisherAction> actions = new ArrayList<>();
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
            BundleDescription desc = getBundleDescription(artifact);
            if (desc != null) {
                if (dependenciesOnly && optionalAction != null) {
                    actions.add(new BundleDependenciesAction(desc, optionalAction));
                } else {
                    actions.add(new BundlesAction(new BundleDescription[] { desc }));
                }
            }
        } else if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
            Feature feature = new FeatureParser().parse(location);
            feature.setLocation(location.getAbsolutePath());
            if (dependenciesOnly) {
                actions.add(new FeatureDependenciesAction(feature));
            } else {
                actions.add(new FeaturesAction(new Feature[] { feature }));
            }
        } else if (PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(packaging)) {
            for (File productFile : getProductFiles(location)) {
                String product = productFile.getAbsolutePath();
                IProductDescriptor productDescriptor;
                try {
                    productDescriptor = new ProductFile2(product);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to parse the product file " + product, e);
                }
                if (dependenciesOnly) {
                    actions.add(new ProductDependenciesAction(productDescriptor));
                }
            }
            for (File categoryFile : getCategoryFiles(location)) {
                CategoryParser cp = new CategoryParser(null);
                try (FileInputStream ins = new FileInputStream(categoryFile)) {
                    SiteModel siteModel = cp.parse(ins);
                    actions.add(
                            new CategoryDependenciesAction(siteModel, artifact.getArtifactId(), artifact.getVersion()));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to read category File", e);
                }
            }
            File p2inf = new File(location, "p2.inf");
            if (p2inf.isFile()) {
                AdviceFileAdvice advice = new AdviceFileAdvice(artifact.getArtifactId(), Version.parseVersion("1.0"),
                        new Path(location.getAbsolutePath()), new Path("p2.inf"));
                if (advice.containsAdvice()) {
                    actions.add(new AbstractPublisherAction() {

                        @Override
                        public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results,
                                IProgressMonitor monitor) {
                            InstallableUnitDescription[] descriptions = advice
                                    .getAdditionalInstallableUnitDescriptions(null);
                            if (descriptions != null && descriptions.length > 0) {
                                for (InstallableUnitDescription desc : descriptions) {
                                    results.addIU(MetadataFactory.createInstallableUnit(desc),
                                            IPublisherResult.NON_ROOT);
                                }
                            }
//                            publisherInfo.addAdvice(advice);
                            return Status.OK_STATUS;
                        }

                    });
                }
            }
        } else if (PackagingType.TYPE_P2_SITE.equals(packaging)) {
            //nothing to do at the moment...
        } else if (PackagingType.TYPE_P2_IU.equals(packaging)) {
            actions.add(new AuthoredIUAction(location));
        } else if (location.isFile() && location.getName().endsWith(".jar")) {
            actions.add(new BundlesAction(new File[] { location }));
        } else {
            throw new IllegalArgumentException("Unknown type of packaging " + packaging);
        }

        return actions;
    }

    private BundleDescription getBundleDescription(IArtifactFacade artifact) {
        File location = artifact.getLocation();
        try {
            if (artifact.getClassifier() == null || artifact.getClassifier().isEmpty()) {
                if (artifact instanceof ReactorProjectFacade) {
                    ReactorProjectFacade projectFacade = (ReactorProjectFacade) artifact;
                    ReactorProject reactorProject = projectFacade.getReactorProject();
                    File manifestLocation = bundleReader.getManifestLocation(reactorProject.adapt(MavenProject.class));
                    if (manifestLocation != null) {
                        CaseInsensitiveDictionaryMap<String, String> headers = new CaseInsensitiveDictionaryMap<>(10);
                        ManifestElement.parseBundleManifest(new FileInputStream(manifestLocation), headers);
                        return BundlesAction.createBundleDescription(headers, location);
                    }
                }
            }
            return BundlesAction.createBundleDescription(location);
        } catch (IOException | BundleException e) {
        }
        return null;
    }

    public boolean isSupported(String type) {
        return Arrays.asList(SUPPORTED_TYPES).contains(type);
    }

    /**
     * Looks for all files at the base of the project that extension is ".product" Duplicated in the
     * EclipseRepositoryProject
     *
     * @param projectLocation
     * @return The list of product files to parse for an eclipse-repository project
     */
    private List<File> getProductFiles(File projectLocation) {
        List<File> res = new ArrayList<>();
        for (File f : projectLocation.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".product") && !f.getName().startsWith(".polyglot")) {
                res.add(f);
            }
        }
        return res;
    }

    private List<File> getCategoryFiles(File projectLocation) {
        List<File> res = new ArrayList<>();
        File categoryFile = new File(projectLocation, "category.xml");
        if (categoryFile.exists()) {
            res.add(categoryFile);
        }
        return res;
    }

    @Override
    protected List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact, PublisherOptions options) {
        BuildProperties buildProperties;
        if (artifact instanceof ReactorProjectFacade reactorFacade) {
            buildProperties = buildPropertiesParser.parse(reactorFacade.getReactorProject());
        } else {
            buildProperties = buildPropertiesParser.parse(artifact.getLocation(), null);
        }
        ArrayList<IPublisherAdvice> advice = new ArrayList<>();
        advice.add(new TychoMavenPropertiesAdvice(artifact, mavenContext));
        advice.add(getExtraEntriesAdvice(artifact, buildProperties));

        if (options.isGenerateDownloadStats()) {
            advice.add(new DownloadStatsAdvice());
        }

        IFeatureRootAdvice featureRootAdvice = FeatureRootAdvice.createRootFileAdvice(artifact,
                getBuildPropertiesParser());
        if (featureRootAdvice != null) {
            advice.add(featureRootAdvice);
        }
        return advice;
    }

    @Override
    protected BuildPropertiesParser getBuildPropertiesParser() {
        return buildPropertiesParser;
    }

    @Override
    public Map<String, IP2Artifact> generateMetadata(MavenProject project, boolean generateDownloadStatsProperty,
            boolean generateChecksums) throws IOException {
        File targetDir = new File(project.getBuild().getDirectory());
        ArtifactFacade projectDefaultArtifact = new ArtifactFacade(project.getArtifact());
        List<IArtifactFacade> artifacts = new ArrayList<>();

        artifacts.add(projectDefaultArtifact);

        for (Artifact attachedArtifact : project.getAttachedArtifacts()) {
            if (attachedArtifact.getFile() != null && (attachedArtifact.getFile().getName().endsWith(".jar")
                    || (attachedArtifact.getFile().getName().endsWith(".zip")
                            && project.getPackaging().equals(ArtifactType.TYPE_INSTALLABLE_UNIT)))) {
                artifacts.add(new ArtifactFacade(attachedArtifact));
            }
        }

        PublisherOptions options = new PublisherOptions();
        options.setGenerateDownloadStats(generateDownloadStatsProperty);
        options.setGenerateChecksums(generateChecksums);
        return generateMetadata(artifacts, options, targetDir);
    }

    @Override
    public FileInfo persistMetadata(Map<String, IP2Artifact> metadata, MavenProject project) throws IOException {
        File targetDir = new File(project.getBuild().getDirectory());
        File contentsXml = new File(targetDir, TychoConstants.FILE_NAME_P2_METADATA);
        File artifactsXml = new File(targetDir, TychoConstants.FILE_NAME_P2_ARTIFACTS);
        persistMetadata(metadata, contentsXml, artifactsXml);
        return new FileInfo(contentsXml, artifactsXml);
    }

    @Override
    public void writeArtifactLocations(MavenProject project) throws IOException {
        File localArtifactsFile = new File(project.getBuild().getDirectory(), TychoConstants.FILE_NAME_LOCAL_ARTIFACTS);
        writeArtifactLocations(localArtifactsFile, getAllProjectArtifacts(project));
    }

    static void writeArtifactLocations(File outputFile, Map<String, File> artifactLocations) throws IOException {
        Properties outputProperties = new Properties();

        for (Entry<String, File> entry : artifactLocations.entrySet()) {
            if (entry.getKey() == null) {
                outputProperties.put(TychoConstants.KEY_ARTIFACT_MAIN, entry.getValue().getAbsolutePath());
            } else {
                outputProperties.put(TychoConstants.KEY_ARTIFACT_ATTACHED + entry.getKey(),
                        entry.getValue().getAbsolutePath());
            }
        }

        writeProperties(outputProperties, outputFile);
    }

    private static void writeProperties(Properties properties, File outputFile) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            properties.store(outputStream, null);
        }
    }

    /**
     * Returns a map from classifiers to artifact files of the given project. The classifier
     * <code>null</code> is mapped to the project's main artifact.
     */
    private static Map<String, File> getAllProjectArtifacts(MavenProject project) {
        Map<String, File> artifacts = new HashMap<>();
        Artifact mainArtifact = project.getArtifact();
        if (mainArtifact != null) {
            artifacts.put(null, mainArtifact.getFile());
        }
        for (Artifact attachedArtifact : project.getAttachedArtifacts()) {
            artifacts.put(attachedArtifact.getClassifier(), attachedArtifact.getFile());
        }
        return artifacts;
    }

    @Override
    public void generateMetaData(MavenProject mavenProject) throws IOException {
        //TODO we probably should get the active execution here and derive the data from the config of the p2 plugin that applies here
        Map<String, IP2Artifact> generatedMetadata = generateMetadata(mavenProject, false, false);
        persistMetadata(generatedMetadata, mavenProject);
        writeArtifactLocations(mavenProject);
    }

}
