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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;
import org.eclipse.tycho.p2.publisher.AbstractMetadataGenerator;
import org.eclipse.tycho.p2.publisher.AuthoredIUAction;
import org.eclipse.tycho.p2.publisher.BundleDependenciesAction;
import org.eclipse.tycho.p2.publisher.CategoryDependenciesAction;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.publisher.DownloadStatsAdvice;
import org.eclipse.tycho.p2.publisher.FeatureDependenciesAction;
import org.eclipse.tycho.p2.publisher.FeatureRootfileArtifactRepository;
import org.eclipse.tycho.p2.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.publisher.P2Artifact;
import org.eclipse.tycho.p2.publisher.ProductDependenciesAction;
import org.eclipse.tycho.p2.publisher.ProductFile2;
import org.eclipse.tycho.p2.publisher.TransientArtifactRepository;
import org.eclipse.tycho.p2.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.repository.ArtifactsIO;
import org.eclipse.tycho.p2.repository.MetadataIO;

@Component(role = P2Generator.class)
public class P2GeneratorImpl extends AbstractMetadataGenerator implements P2Generator {
    private static final String[] SUPPORTED_TYPES = { PackagingType.TYPE_ECLIPSE_PLUGIN,
            PackagingType.TYPE_ECLIPSE_TEST_PLUGIN, PackagingType.TYPE_ECLIPSE_FEATURE,
            PackagingType.TYPE_ECLIPSE_REPOSITORY };

    /**
     * Whether we need full p2 metadata (false) or just required capabilities.
     */
    private boolean dependenciesOnly;
    @Requirement
    private MavenContext mavenContext;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    private Logger logger;

    public P2GeneratorImpl(boolean dependenciesOnly) {
        this.dependenciesOnly = dependenciesOnly;
    }

    // no-args constructor required by DS
    public P2GeneratorImpl() {
        this(false);
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
                publisherInfo.setArtifactOptions(
                        IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH | IPublisherInfo.A_NO_MD5);
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
                publisherInfo.setArtifactOptions(IPublisherInfo.A_PUBLISH | IPublisherInfo.A_NO_MD5);
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
                        + ", as it does not has a canonical ArtifactDescriptor");
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
            if (dependenciesOnly && optionalAction != null) {
                actions.add(new BundleDependenciesAction(location, optionalAction));
            } else {
                actions.add(new BundlesAction(new File[] { location }));
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
        } else if (PackagingType.TYPE_P2_IU.equals(packaging)) {
            actions.add(new AuthoredIUAction(location));
        } else if (location.isFile() && location.getName().endsWith(".jar")) {
            actions.add(new BundlesAction(new File[] { location }));
        } else {
            throw new IllegalArgumentException("Unknown type of packaging " + packaging);
        }

        return actions;
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
        Interpolator interpolator = artifact instanceof ReactorProjectFacade reactorFacade
                ? reactorFacade.getReactorProject().getInterpolator()
                : null;
        ArrayList<IPublisherAdvice> advice = new ArrayList<>();
        advice.add(new MavenPropertiesAdvice(artifact, mavenContext));
        advice.add(getExtraEntriesAdvice(artifact, interpolator));

        if (options.generateDownloadStatsProperty) {
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

    public void setMavenContext(MavenContext mockMavenContext) {
        mavenContext = mockMavenContext;
    }

    public void setBuildPropertiesParser(BuildPropertiesParser propertiesParserForTesting) {
        buildPropertiesParser = propertiesParserForTesting;
    }

}
