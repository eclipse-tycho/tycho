/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.MavenGAVLocation.MissingManifestStrategy;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.repository.FileArtifactRepository;
import org.eclipse.tycho.p2.target.repository.SupplierMetadataRepository;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class MavenTargetDefinitionContent {

    private final Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<IArtifactDescriptor, IInstallableUnit>();
    private SupplierMetadataRepository metadataRepository;
    private FileArtifactRepository artifactRepository;

    public MavenTargetDefinitionContent(MavenGAVLocation location, MavenDependenciesResolver mavenDependenciesResolver,
            IProvisioningAgent agent, MavenLogger logger, Set<String> wrappedFiles) {
        File repositoryRoot = mavenDependenciesResolver.getRepositoryRoot();
        metadataRepository = new SupplierMetadataRepository(agent, () -> repositoryContent.values().iterator());
        metadataRepository.setLocation(repositoryRoot.toURI());
        metadataRepository.setName(repositoryRoot.getName());
        artifactRepository = new FileArtifactRepository(agent, () -> repositoryContent.keySet().iterator());
        artifactRepository.setName(repositoryRoot.getName());
        artifactRepository.setLocation(repositoryRoot.toURI());

        if (mavenDependenciesResolver != null) {
            logger.info("Resolving " + location + "...");
            Collection<?> resolve = mavenDependenciesResolver.resolve(location.getGroupId(), location.getArtifactId(),
                    location.getVersion(), location.getIncludeDependencyScope(), location.getArtifactType());

            Iterator<IArtifactFacade> resolvedArtifacts = resolve.stream().filter(IArtifactFacade.class::isInstance)
                    .map(IArtifactFacade.class::cast).iterator();
            while (resolvedArtifacts.hasNext()) {
                IArtifactFacade mavenArtifact = (IArtifactFacade) resolvedArtifacts.next();
                logger.debug("Resolved " + mavenArtifact + "...");
                try {
                    File bundleLocation = mavenArtifact.getLocation();
                    BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleLocation);
                    if (bundleDescription == null) {
                        throw new TargetDefinitionResolutionException(
                                "Artifact " + mavenArtifact + " of location " + location + " is not a valid jar file");
                    } else {
                        String symbolicName = bundleDescription.getSymbolicName();
                        IInstallableUnit unit;
                        if (symbolicName == null) {
                            if (wrappedFiles.contains(bundleLocation.getAbsolutePath())) {
                                //already provided by another location
                                continue;
                            }
                            if (location.getMissingManifestStrategy() == MissingManifestStrategy.IGNORE) {
                                logger.info("Ignoring " + asDebugString(mavenArtifact)
                                        + " as it is not a bundle and MissingManifestStrategy is set to ignore for this location");
                                continue;
                            }
                            if (location.getMissingManifestStrategy() == MissingManifestStrategy.ERROR) {
                                throw new TargetDefinitionResolutionException("Artifact " + asDebugString(mavenArtifact)
                                        + " is not a bundle and MissingManifestStrategy is set to error for this location");
                            }
                            File tempFile = File.createTempFile("tycho_wrapped_bundle", ".jar");
                            tempFile.deleteOnExit();
                            WrappedArtifact wrappedArtifact;
                            try {
                                wrappedArtifact = WrappedArtifact.createWrappedArtifact(mavenArtifact, "wrapped",
                                        tempFile);
                            } catch (Exception e) {
                                throw new TargetDefinitionResolutionException(
                                        "Artifact " + asDebugString(mavenArtifact) + " could not be wrapped", e);
                            }
                            logger.info(
                                    asDebugString(mavenArtifact) + " is wrapped as a bundle with bundle symbolic name "
                                            + wrappedArtifact.getWrappedBsn());
                            logger.info(wrappedArtifact.getReferenceHint());
                            if (logger.isDebugEnabled()) {
                                logger.debug("The follwoing manifest was generated for this artifact:\r\n"
                                        + wrappedArtifact.getGeneratedManifest());
                            }
                            unit = publish(BundlesAction.createBundleDescription(tempFile), tempFile);
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
            }
        }
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

    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

}
