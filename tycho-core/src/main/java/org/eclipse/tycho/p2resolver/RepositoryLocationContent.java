/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.core.resolver.target.SupplierMetadataRepository;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.resolver.BundlePublisher;
import org.eclipse.tycho.p2.resolver.FeaturePublisher;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.p2maven.transport.TychoRepositoryTransport;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;

public class RepositoryLocationContent implements TargetDefinitionContent {
    private final Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<>();
    private SupplierMetadataRepository metadataRepository;
    private FileArtifactRepository artifactRepository;

    public RepositoryLocationContent(URI uri, Collection<Requirement> requirements, IProvisioningAgent agent,
            MavenLogger logger) throws TargetDefinitionResolutionException {
        TychoRepositoryTransport tychoTransport = (TychoRepositoryTransport) agent.getService(Transport.class);

        metadataRepository = new SupplierMetadataRepository(agent, () -> repositoryContent.values().iterator());
        metadataRepository.setLocation(uri);
        metadataRepository.setName(String.valueOf(uri));
        artifactRepository = new FileArtifactRepository(agent, () -> repositoryContent.keySet().stream()
                .filter(Predicate.not(FeaturePublisher::isMetadataOnly)).iterator());
        artifactRepository.setName(String.valueOf(uri));
        artifactRepository.setLocation(uri);
        List<Feature> features = new ArrayList<>();
        ResourcesRepository repository;
        try (InputStream stream = tychoTransport.stream(uri, null)) {
            repository = new ResourcesRepository(XMLResourceParser.getResources(stream, uri));
        } catch (Exception e) {
            throw new TargetDefinitionResolutionException("Can't load the repository from URI " + uri, e);
        }
        Map<Requirement, Collection<Capability>> providers = repository.findProviders(requirements);
        //TODO once we have changed Tycho to use resources this can be optimized to not download all selected content here ...
        List<ContentCapability> contentCapabilities = providers.values().stream().flatMap(Collection::stream)
                .map(Capability::getResource).distinct().map(ResourceUtils::getContentCapability)
                .filter(Objects::nonNull).toList();
        for (ContentCapability content : contentCapabilities) {
            URI url = content.url();
            logger.info("Loading " + url + "...");
            try {
                File file = tychoTransport.downloadToFile(url);
                if (!"jar".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
                    logger.info("Skip non-jar artifact (" + file + ")");
                    continue;
                }
                Feature feature = new FeatureParser().parse(file);
                if (feature != null) {
                    feature.setLocation(file.getAbsolutePath());
                    features.add(feature);
                    continue;
                }
                BundleDescription bundleDescription = BundlesAction.createBundleDescription(file);
                if (bundleDescription == null || bundleDescription.getSymbolicName() == null) {
                    continue;
                }
                publish(bundleDescription, file);
            } catch (Exception e) {
                throw new TargetDefinitionResolutionException("Can't fetch resource from " + url, e);
            }
        }
        FeaturePublisher.publishFeatures(features, repositoryContent::put, artifactRepository, logger);
    }

    private void publish(BundleDescription bundleDescription, File bundleLocation) {
        IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
                bundleDescription.getVersion().toString());
        IArtifactDescriptor descriptor = FileArtifactRepository.forFile(bundleLocation, key, artifactRepository);
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        IInstallableUnit iu = BundlePublisher.publishBundle(bundleDescription, descriptor, publisherInfo);
        repositoryContent.put(descriptor, iu);
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

}
