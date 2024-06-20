/*******************************************************************************
 * Copyright (c) 2020, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.m2e.pde.target.shared.MavenBundleWrapper;
import org.eclipse.m2e.pde.target.shared.ProcessingMessage;
import org.eclipse.m2e.pde.target.shared.WrappedBundle;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.DependencyResolutionException;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.core.MavenModelFacade;
import org.eclipse.tycho.core.publisher.TychoMavenPropertiesAdvice;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.core.resolver.target.SupplierMetadataRepository;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.osgi.configuration.MavenDependenciesResolverConfigurer;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.resolver.BundlePublisher;
import org.eclipse.tycho.p2.resolver.FeatureGenerator;
import org.eclipse.tycho.p2.resolver.FeaturePublisher;
import org.eclipse.tycho.p2.resolver.WrappedArtifact;
import org.eclipse.tycho.p2maven.advices.MavenChecksumAdvice;
import org.eclipse.tycho.p2maven.advices.MavenPropertiesAdvice;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.targetplatform.TargetDefinition.BNDInstructions;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenDependency;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.DependencyDepth;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.MissingManifestStrategy;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class MavenTargetDefinitionContent implements TargetDefinitionContent {
    private static final String POM_PACKAGING_TYPE = "pom";
    public static final String ECLIPSE_SOURCE_BUNDLE_HEADER = "Eclipse-SourceBundle";
    private final Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<>();
    private SupplierMetadataRepository metadataRepository;
    private FileArtifactRepository artifactRepository;
    private MavenContext mavenContext;

    public MavenTargetDefinitionContent(MavenGAVLocation location, MavenDependenciesResolver mavenDependenciesResolver,
            IncludeSourceMode sourceMode, IProvisioningAgent agent, MavenContext mavenContext,
            SyncContextFactory syncContextFactory, RepositorySystem repositorySystem, MavenSession mavenSession,
            org.eclipse.aether.RepositorySystem repositorySystem2) {
        this.mavenContext = mavenContext;
        MavenLogger logger = mavenContext.getLogger();
        File repositoryRoot = mavenDependenciesResolver.getRepositoryRoot();
        boolean includeSource = sourceMode == IncludeSourceMode.force
                || (sourceMode == IncludeSourceMode.honor && location.includeSource());
        metadataRepository = new SupplierMetadataRepository(agent, () -> repositoryContent.values().iterator());
        metadataRepository.setLocation(repositoryRoot.toURI());
        metadataRepository.setName(repositoryRoot.getName());
        artifactRepository = new FileArtifactRepository(agent, () -> repositoryContent.keySet().stream()
                .filter(Predicate.not(FeaturePublisher::isMetadataOnly)).iterator());
        artifactRepository.setName(repositoryRoot.getName());
        artifactRepository.setLocation(repositoryRoot.toURI());
        Collection<BNDInstructions> instructions = location.getInstructions();
        List<Feature> features = new ArrayList<>();
        if (mavenDependenciesResolver != null) {
            logger.info("Resolving " + location);
            Map<String, Properties> instructionsMap = new HashMap<>();
            for (BNDInstructions instruction : instructions) {
                String reference = instruction.getReference();
                Properties properties = instruction.getInstructions();
                instructionsMap.put(reference, properties);
                logger.info((reference.isEmpty() ? "default instructions" : reference) + " = " + properties);
            }
            List<IInstallableUnit> locationBundles = new ArrayList<>();
            List<IInstallableUnit> locationSourceBundles = new ArrayList<>();
            for (MavenDependency mavenDependency : location.getRoots()) {
                DependencyDepth dependencyDepth = location.getIncludeDependencyDepth();
                if (dependencyDepth == DependencyDepth.NONE
                        && POM_PACKAGING_TYPE.equalsIgnoreCase(mavenDependency.getArtifactType())) {
                    dependencyDepth = DependencyDepth.DIRECT;
                }
                int depth = switch (dependencyDepth) {
                case INFINITE -> MavenDependenciesResolver.DEEP_INFINITE;
                case DIRECT -> MavenDependenciesResolver.DEEP_DIRECT_CHILDREN;
                default -> MavenDependenciesResolver.DEEP_NO_DEPENDENCIES;
                };
                Collection<?> resolve;
                try {
                    resolve = mavenDependenciesResolver.resolve(mavenDependency.getGroupId(),
                            mavenDependency.getArtifactId(), mavenDependency.getVersion(),
                            mavenDependency.getArtifactType(), mavenDependency.getClassifier(),
                            location.getIncludeDependencyScopes(), depth, location.getRepositoryReferences());
                } catch (DependencyResolutionException e1) {
                    throw new TargetDefinitionResolutionException("MavenDependency " + mavenDependency + " of location "
                            + location + " could not be resolved", e1);
                }

                Iterator<IArtifactFacade> resolvedArtifacts = resolve.stream().filter(IArtifactFacade.class::isInstance)
                        .map(IArtifactFacade.class::cast).iterator();
                Properties defaultProperties = WrappedArtifact.createPropertiesForPrefix("wrapped");
                List<IInstallableUnit> bundles = new ArrayList<>();
                List<IInstallableUnit> sourceBundles = new ArrayList<>();
                while (resolvedArtifacts.hasNext()) {
                    IArtifactFacade mavenArtifact = resolvedArtifacts.next();
                    if (mavenDependency.isIgnored(mavenArtifact)) {
                        logger.debug("Skip ignored " + mavenArtifact);
                        continue;
                    }
                    if (POM_PACKAGING_TYPE.equalsIgnoreCase(mavenArtifact.getPackagingType())) {
                        logger.debug("Skip pom artifact " + mavenArtifact);
                        continue;
                    }
                    String fileName = mavenArtifact.getLocation().getName();
                    if (!"jar".equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
                        logger.info("Skip non-jar artifact (" + fileName + ")");
                        continue;
                    }
                    logger.debug("Resolved " + mavenArtifact);

                    Feature feature = new FeatureParser().parse(mavenArtifact.getLocation());
                    if (feature != null) {
                        feature.setLocation(mavenArtifact.getLocation().getAbsolutePath());
                        features.add(feature);
                        continue;
                    }

                    String symbolicName;
                    String bundleVersion;
                    try {
                        File bundleLocation = mavenArtifact.getLocation();
                        BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleLocation);
                        symbolicName = bundleDescription != null ? bundleDescription.getSymbolicName() : null;
                        bundleVersion = bundleDescription != null ? bundleDescription.getVersion().toString() : null;
                        IInstallableUnit unit;
                        if (symbolicName == null) {
                            if (location.getMissingManifestStrategy() == MissingManifestStrategy.IGNORE) {
                                logger.info("Ignoring " + asDebugString(mavenArtifact)
                                        + " as it is not a bundle and MissingManifestStrategy is set to ignore for this location");
                                continue;
                            }
                            if (location.getMissingManifestStrategy() == MissingManifestStrategy.ERROR) {
                                throw new TargetDefinitionResolutionException("Artifact " + asDebugString(mavenArtifact)
                                        + " is not a bundle and MissingManifestStrategy is set to error for this location");
                            }
                            try {
                                List<RemoteRepository> repositories = RepositoryUtils
                                        .toRepos(MavenDependenciesResolverConfigurer.getEffectiveRepositories(
                                                mavenSession.getCurrentProject(), location.getRepositoryReferences(),
                                                repositorySystem));
                                Function<DependencyNode, Properties> instructionsLookup = node -> instructionsMap
                                        .getOrDefault(getKey(node.getArtifact()),
                                                instructionsMap.getOrDefault("", defaultProperties));
                                WrappedBundle wrappedBundle = MavenBundleWrapper.getWrappedArtifact(
                                        new DefaultArtifact(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(),
                                                mavenArtifact.getClassifier(), mavenArtifact.getPackagingType(),
                                                mavenArtifact.getVersion()),
                                        instructionsLookup, repositories, repositorySystem2,
                                        mavenSession.getRepositorySession(), syncContextFactory);
                                List<ProcessingMessage> directErrors = wrappedBundle.messages(false)
                                        .filter(msg -> msg.type() == ProcessingMessage.Type.ERROR).toList();
                                if (directErrors.isEmpty()) {
                                    wrappedBundle.messages(true).map(ProcessingMessage::message)
                                            .forEach(msg -> logger.warn(asDebugString(mavenArtifact) + ": " + msg));
                                } else {
                                    throw new RuntimeException(directErrors.stream().map(ProcessingMessage::message)
                                            .collect(Collectors.joining(System.lineSeparator())));
                                }
                                File file = wrappedBundle.getFile().get().toFile();
                                BundleDescription description = BundlesAction.createBundleDescription(file);
                                WrappedArtifact wrappedArtifact = new WrappedArtifact(file, mavenArtifact,
                                        mavenArtifact.getClassifier(), description.getSymbolicName(),
                                        description.getVersion().toString(), null);
                                logger.info(asDebugString(mavenArtifact)
                                        + " is wrapped as a bundle with bundle symbolic name "
                                        + wrappedArtifact.getWrappedBsn());
                                logger.info(wrappedArtifact.getReferenceHint());
                                if (logger.isDebugEnabled()) {
                                    logger.debug("The following manifest was generated for this artifact:\r\n"
                                            + wrappedArtifact.getGeneratedManifest());
                                }
                                // Maven artifact info for wrapped bundles have to be stored in separate fields
                                Map<String, String> mavenProperties = new HashMap<>();
                                mavenProperties.put(TychoConstants.PROP_WRAPPED_GROUP_ID, mavenArtifact.getGroupId());
                                mavenProperties.put(TychoConstants.PROP_WRAPPED_ARTIFACT_ID,
                                        mavenArtifact.getArtifactId());
                                mavenProperties.put(TychoConstants.PROP_WRAPPED_VERSION, mavenArtifact.getVersion());
                                mavenProperties.put(TychoConstants.PROP_WRAPPED_CLASSIFIER,
                                        mavenArtifact.getClassifier());
                                unit = publish(description, file, new MavenPropertiesAdvice(mavenProperties));
                                symbolicName = wrappedArtifact.getWrappedBsn();
                                bundleVersion = wrappedArtifact.getWrappedVersion();
                            } catch (Exception e) {
                                throw new TargetDefinitionResolutionException("Artifact " + asDebugString(mavenArtifact)
                                        + " of location " + location + " could not be wrapped as a bundle", e);
                            }

                        } else {
                            unit = publish(bundleDescription, bundleLocation, mavenArtifact);
                        }
                        bundles.add(unit);
                        if (logger.isDebugEnabled()) {
                            logger.debug("MavenResolver: artifact " + asDebugString(mavenArtifact) + " at location "
                                    + bundleLocation + " resolves installable unit "
                                    + new VersionedId(unit.getId(), unit.getVersion()));
                        }
                    } catch (BundleException | IOException e) {
                        throw new TargetDefinitionResolutionException("Artifact " + asDebugString(mavenArtifact)
                                + " of location " + location + " could not be read", e);
                    }

                    if (includeSource) {
                        try {
                            Collection<?> sourceArtifacts = mavenDependenciesResolver.resolve(
                                    mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(),
                                    mavenArtifact.getVersion(), mavenArtifact.getPackagingType(), "sources", null,
                                    MavenDependenciesResolver.DEEP_NO_DEPENDENCIES, location.getRepositoryReferences());
                            Iterator<IArtifactFacade> sources = sourceArtifacts.stream()
                                    .filter(IArtifactFacade.class::isInstance).map(IArtifactFacade.class::cast)
                                    .iterator();
                            while (sources.hasNext()) {
                                IArtifactFacade sourceArtifact = sources.next();
                                File sourceFile = sourceArtifact.getLocation();
                                try {
                                    Manifest manifest;
                                    try (JarFile jar = new JarFile(sourceFile)) {
                                        manifest = Objects.requireNonNullElseGet(jar.getManifest(), Manifest::new);
                                    }
                                    IInstallableUnit unit;
                                    if (isValidSourceManifest(manifest)) {
                                        unit = publish(BundlesAction.createBundleDescription(sourceFile), sourceFile,
                                                sourceArtifact);
                                    } else {
                                        unit = generateSourceBundle(symbolicName, bundleVersion, manifest, sourceFile,
                                                sourceArtifact);
                                    }
                                    sourceBundles.add(unit);
                                    if (unit != null && logger.isDebugEnabled()) {
                                        logger.debug("MavenResolver: source-artifact " + asDebugString(sourceArtifact)
                                                + ":sources at location " + sourceFile + " resolves installable unit "
                                                + new VersionedId(unit.getId(), unit.getVersion()));
                                    }
                                } catch (IOException | BundleException e) {
                                    logger.warn("MavenResolver: source-artifact " + asDebugString(sourceArtifact)
                                            + ":sources at location " + sourceFile
                                            + " cannot be converted to a source bundle: " + e);
                                    continue;
                                }
                            }
                        } catch (DependencyResolutionException e) {
                            logger.warn("MavenResolver: source-artifact " + asDebugString(mavenArtifact)
                                    + ":sources cannot be resolved: " + e);
                        }
                    }
                }
                if (POM_PACKAGING_TYPE.equalsIgnoreCase(mavenDependency.getArtifactType())) {
                    Optional<File> pomFacade = resolve.stream().filter(IArtifactFacade.class::isInstance)
                            .map(IArtifactFacade.class::cast).filter(facade -> facade.getDependencyTrail().size() == 1)
                            .filter(facade -> facade.getArtifactId().equals(mavenDependency.getArtifactId())
                                    && facade.getGroupId().equals(mavenDependency.getGroupId())
                                    && facade.getVersion().equals(mavenDependency.getVersion())
                                    && facade.getPackagingType().equals(POM_PACKAGING_TYPE))
                            .map(IArtifactFacade::getLocation).filter(Objects::nonNull).findFirst();

                    if (pomFacade.isPresent()) {
                        try {
                            MavenModelFacade model = mavenDependenciesResolver.loadModel(pomFacade.get());
                            features.add(FeatureGenerator.generatePomFeature(model, bundles, false, logger));
                            if (includeSource) {
                                features.add(FeatureGenerator.generatePomFeature(model, sourceBundles, true, logger));
                            }
                        } catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
                            throw new TargetDefinitionResolutionException("non readable pom file");
                        }
                    }
                }
                locationBundles.addAll(bundles);
                locationSourceBundles.addAll(sourceBundles);
            }
            Element featureTemplate = location.getFeatureTemplate();
            if (featureTemplate != null) {
                try {
                    features.add(FeatureGenerator.createFeatureFromTemplate(featureTemplate, locationBundles, false,
                            logger));
                    if (includeSource) {
                        features.add(FeatureGenerator.createFeatureFromTemplate(featureTemplate, locationSourceBundles,
                                true, logger));
                    }
                } catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
                    throw new TargetDefinitionResolutionException("feature generation failed!", e);
                }
            }
            FeaturePublisher.publishFeatures(features, repositoryContent::put, artifactRepository, logger);
        }
    }

    private IInstallableUnit generateSourceBundle(String symbolicName, String bundleVersion, Manifest manifest,
            File sourceFile, IArtifactFacade sourceArtifact) throws IOException, BundleException {

        File tempFile = File.createTempFile("tycho_wrapped_source", ".jar");
        tempFile.deleteOnExit();
        Attributes attr = manifest.getMainAttributes();
        if (attr.isEmpty()) {
            attr.put(Name.MANIFEST_VERSION, "1.0");
        }
        attr.putValue(ECLIPSE_SOURCE_BUNDLE_HEADER, symbolicName + ";version=\"" + bundleVersion + "\";roots:=\".\"");
        attr.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attr.putValue(Constants.BUNDLE_NAME, "Source Bundle for " + symbolicName + ":" + bundleVersion);
        attr.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName + ".source");
        attr.putValue(Constants.BUNDLE_VERSION, bundleVersion);
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(tempFile), manifest)) {
            try (JarFile jar = new JarFile(sourceFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    if (jarEntry.getName().equals(JarFile.MANIFEST_NAME)) {
                        continue;
                    }
                    try (InputStream is = jar.getInputStream(jarEntry)) {
                        stream.putNextEntry(new ZipEntry(jarEntry.getName()));
                        is.transferTo(stream);
                        stream.closeEntry();
                    }
                }
            }
        }
        return publish(BundlesAction.createBundleDescription(tempFile), tempFile, sourceArtifact);

    }

    private IInstallableUnit publish(BundleDescription bundleDescription, File bundleLocation,
            IArtifactFacade mavenArtifact) {
        return publish(bundleDescription, bundleLocation, new TychoMavenPropertiesAdvice(mavenArtifact, mavenContext));
    }

    private IInstallableUnit publish(BundleDescription bundleDescription, File bundleLocation, IPropertyAdvice advice) {
        IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
                bundleDescription.getVersion().toString());
        IArtifactDescriptor descriptor = FileArtifactRepository.forFile(bundleLocation, key, artifactRepository);
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.addAdvice(advice);
        publisherInfo.addAdvice(new MavenChecksumAdvice(bundleLocation));
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        IInstallableUnit iu = BundlePublisher.publishBundle(bundleDescription, descriptor, publisherInfo);
        repositoryContent.put(descriptor, iu);
        return iu;
    }

    private String asDebugString(IArtifactFacade mavenArtifact) {
        return new GAV(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion())
                .toString();
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

    private static String getKey(Artifact artifact) {
        if (artifact == null) {
            return "";
        }
        String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
        String classifier = artifact.getClassifier();
        if (classifier != null) {
            key += ":" + classifier;
        }
        key += ":" + artifact.getVersion();
        return key;
    }

    private static boolean isValidSourceManifest(Manifest manifest) {
        if (manifest != null) {
            return manifest.getMainAttributes().getValue(ECLIPSE_SOURCE_BUNDLE_HEADER) != null;
        }
        return false;
    }

}
