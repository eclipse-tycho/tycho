/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.TargetDefinitionContent;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.BNDInstructions;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenDependency;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation.MissingManifestStrategy;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.repository.FileArtifactRepository;
import org.eclipse.tycho.p2.target.repository.SupplierMetadataRepository;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

@SuppressWarnings("restriction")
public class MavenTargetDefinitionContent implements TargetDefinitionContent {
    public static final String ECLIPSE_SOURCE_BUNDLE_HEADER = "Eclipse-SourceBundle";
    private final Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<IArtifactDescriptor, IInstallableUnit>();
    private SupplierMetadataRepository metadataRepository;
    private FileArtifactRepository artifactRepository;

    public MavenTargetDefinitionContent(MavenGAVLocation location, MavenDependenciesResolver mavenDependenciesResolver,
            IncludeSourceMode sourceMode, IProvisioningAgent agent, MavenLogger logger) {
        File repositoryRoot = mavenDependenciesResolver.getRepositoryRoot();
        metadataRepository = new SupplierMetadataRepository(agent, () -> repositoryContent.values().iterator());
        metadataRepository.setLocation(repositoryRoot.toURI());
        metadataRepository.setName(repositoryRoot.getName());
        artifactRepository = new FileArtifactRepository(agent, () -> repositoryContent.keySet().iterator());
        artifactRepository.setName(repositoryRoot.getName());
        artifactRepository.setLocation(repositoryRoot.toURI());
        Collection<BNDInstructions> instructions = location.getInstructions();

        if (mavenDependenciesResolver != null) {
            logger.info("Resolving " + location + "...");
            Map<String, Properties> instructionsMap = new HashMap<>();
            for (BNDInstructions instruction : instructions) {
                String reference = instruction.getReference();
                Properties properties = instruction.getInstructions();
                instructionsMap.put(reference, properties);
                logger.info((reference.isEmpty() ? "default instructions" : reference) + " = " + properties);
            }
            for (MavenDependency mavenDependency : location.getRoots()) {
                Collection<?> resolve = mavenDependenciesResolver.resolve(mavenDependency.getGroupId(),
                        mavenDependency.getArtifactId(), mavenDependency.getVersion(),
                        mavenDependency.getArtifactType(), mavenDependency.getClassifier(),
                        location.getIncludeDependencyScope());

                Iterator<IArtifactFacade> resolvedArtifacts = resolve.stream().filter(IArtifactFacade.class::isInstance)
                        .map(IArtifactFacade.class::cast).iterator();
                Properties defaultProperties = WrappedArtifact.createPropertiesForPrefix("wrapped");
                while (resolvedArtifacts.hasNext()) {
                    IArtifactFacade mavenArtifact = resolvedArtifacts.next();
                    logger.debug("Resolved " + mavenArtifact + "...");
                    String symbolicName;
                    String bundleVersion;
                    try {
                        File bundleLocation = mavenArtifact.getLocation();
                        BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleLocation);
                        if (bundleDescription == null) {
                            throw new TargetDefinitionResolutionException("Artifact " + mavenArtifact + " of location "
                                    + location + " is not a valid jar file");
                        } else {
                            symbolicName = bundleDescription.getSymbolicName();
                            bundleVersion = bundleDescription.getVersion().toString();
                            IInstallableUnit unit;
                            if (symbolicName == null) {
                                if (location.getMissingManifestStrategy() == MissingManifestStrategy.IGNORE) {
                                    logger.info("Ignoring " + asDebugString(mavenArtifact)
                                            + " as it is not a bundle and MissingManifestStrategy is set to ignore for this location");
                                    continue;
                                }
                                if (location.getMissingManifestStrategy() == MissingManifestStrategy.ERROR) {
                                    throw new TargetDefinitionResolutionException("Artifact "
                                            + asDebugString(mavenArtifact)
                                            + " is not a bundle and MissingManifestStrategy is set to error for this location");
                                }
                                File tempFile = File.createTempFile("tycho_wrapped_bundle", ".jar");
                                tempFile.deleteOnExit();
                                WrappedArtifact wrappedArtifact;
                                try {
                                    Properties properties = instructionsMap.getOrDefault(getKey(mavenArtifact),
                                            instructionsMap.getOrDefault("", defaultProperties));
                                    wrappedArtifact = WrappedArtifact.createWrappedArtifact(mavenArtifact, properties,
                                            tempFile);
                                } catch (Exception e) {
                                    throw new TargetDefinitionResolutionException(
                                            "Artifact " + asDebugString(mavenArtifact) + " could not be wrapped", e);
                                }
                                logger.info(asDebugString(mavenArtifact)
                                        + " is wrapped as a bundle with bundle symbolic name "
                                        + wrappedArtifact.getWrappedBsn());
                                logger.info(wrappedArtifact.getReferenceHint());
                                if (logger.isDebugEnabled()) {
                                    logger.debug("The follwoing manifest was generated for this artifact:\r\n"
                                            + wrappedArtifact.getGeneratedManifest());
                                }
                                unit = publish(BundlesAction.createBundleDescription(tempFile), tempFile);
                                symbolicName = wrappedArtifact.getWrappedBsn();
                                bundleVersion = wrappedArtifact.getWrappedVersion();
                            } else {
                                unit = publish(bundleDescription, bundleLocation);
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("MavenResolver: artifact " + asDebugString(mavenArtifact) + " at location "
                                        + bundleLocation + " resolves installable unit "
                                        + new VersionedId(unit.getId(), unit.getVersion()));
                            }
                        }
                    } catch (BundleException | IOException e) {
                        throw new TargetDefinitionResolutionException("Artifact " + asDebugString(mavenArtifact)
                                + " of location " + location + " could not be read", e);
                    }
                    if (sourceMode == IncludeSourceMode.force
                            || (sourceMode == IncludeSourceMode.honor && location.includeSource())) {
                        Collection<?> sourceArtifacts = mavenDependenciesResolver.resolve(mavenArtifact.getGroupId(),
                                mavenArtifact.getArtifactId(), mavenArtifact.getVersion(),
                                mavenArtifact.getPackagingType(), "sources", null);
                        Iterator<IArtifactFacade> sources = sourceArtifacts.stream()
                                .filter(IArtifactFacade.class::isInstance).map(IArtifactFacade.class::cast).iterator();
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
                                    unit = publish(BundlesAction.createBundleDescription(sourceFile), sourceFile);
                                } else {
                                    unit = generateSourceBundle(symbolicName, bundleVersion, manifest, sourceFile);
                                }
                                if (unit != null && logger.isDebugEnabled()) {
                                    logger.debug("MavenResolver: source-artifact " + asDebugString(sourceArtifact)
                                            + ":sources at location " + sourceFile + " resolves installable unit "
                                            + new VersionedId(unit.getId(), unit.getVersion()));
                                }
                            } catch (IOException | BundleException e) {
                                logger.warn("MavenResolver: source-artifact " + asDebugString(sourceArtifact)
                                        + ":sources at location " + sourceFile
                                        + " can't be converted to a source bundle: " + e);
                                continue;
                            }
                        }
                    }
                }
            }

        }
    }

    private IInstallableUnit generateSourceBundle(String symbolicName, String bundleVersion, Manifest manifest,
            File sourceFile) throws IOException, BundleException {

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
        return publish(BundlesAction.createBundleDescription(tempFile), tempFile);

    }

    private IInstallableUnit publish(BundleDescription bundleDescription, File bundleLocation) {
        IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
                bundleDescription.getVersion().toString());
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        IInstallableUnit iu = BundlesAction.createBundleIU(bundleDescription, key, publisherInfo);
        repositoryContent.put(FileArtifactRepository.forFile(bundleLocation, key), iu);
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

    private static String getKey(IArtifactFacade artifact) {
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
