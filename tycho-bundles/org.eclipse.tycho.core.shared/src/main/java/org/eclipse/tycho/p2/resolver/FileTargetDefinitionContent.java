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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.core.resolver.target.FileArtifactRepository;
import org.eclipse.tycho.core.resolver.target.SupplierMetadataRepository;
import org.eclipse.tycho.core.resolver.target.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.osgi.framework.BundleException;

public class FileTargetDefinitionContent implements TargetDefinitionContent {

    private Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<IArtifactDescriptor, IInstallableUnit>();
    private final SupplierMetadataRepository metadataRepository;
    private final FileArtifactRepository artifactRepository;
    private File location;

    private boolean loaded;

    public FileTargetDefinitionContent(IProvisioningAgent agent, File location) {
        this.location = location;
        metadataRepository = new SupplierMetadataRepository(agent, () -> getRepositoryContent().values().iterator());
        metadataRepository.setLocation(location.toURI());
        metadataRepository.setName(location.getName());
        artifactRepository = new FileArtifactRepository(agent, () -> getRepositoryContent().keySet().iterator());
        artifactRepository.setName(location.getName());
        artifactRepository.setLocation(location.toURI());
    }

    public IMetadataRepository getMetadataRepository() {
        preload(null);
        return metadataRepository;
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 200);
        preload(subMonitor.split(100));
        subMonitor.setWorkRemaining(100);
        return getMetadataRepository().query(query, subMonitor.split(100));
    }

    public synchronized void preload(IProgressMonitor monitor) {
        if (loaded) {
            return;
        }
        try {
            SubMonitor subMonitor = SubMonitor.convert(monitor, 200);
            File pluginsPath = new File(location, "plugins");
            File featurePath = new File(location, "features");
            boolean hasPlugins = pluginsPath.isDirectory();
            boolean hasFeatures = featurePath.isDirectory();
            if (hasPlugins) {
                readBundles(pluginsPath, repositoryContent::put, hasFeatures ? subMonitor.split(100) : subMonitor);
            }
            if (hasFeatures) {
                readFeatures(featurePath, repositoryContent::put, hasPlugins ? subMonitor.split(100) : subMonitor);
            }
            if (!hasFeatures && !hasPlugins) {
                readBundles(location, repositoryContent::put, subMonitor.split(100));
                readFeatures(location, repositoryContent::put, subMonitor.split(100));
            }
        } catch (ResolverException e) {
            throw new TargetDefinitionResolutionException("resolving location " + location + " failed", e);

        }
        loaded = true;
    }

    public IArtifactRepository getArtifactRepository() {
        preload(null);
        return artifactRepository;
    }

    private Map<IArtifactDescriptor, IInstallableUnit> getRepositoryContent() {
        return repositoryContent;
    }

    private static void readFeatures(File path, BiConsumer<IArtifactDescriptor, IInstallableUnit> consumer,
            IProgressMonitor monitor) throws ResolverException {
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                SubMonitor subMonitor = SubMonitor.convert(monitor, "reading features from path " + path + "...",
                        files.length);
                for (File featureLocation : files) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String name = featureLocation.getName();
                    if (name.startsWith(".") || featureLocation.isFile() && !name.toLowerCase().endsWith(".jar")) {
                        continue;
                    }
                    subMonitor.subTask("Reading " + name);
                    Feature feature = new FeatureParser().parse(featureLocation);
                    if (feature != null) {
                        feature.setLocation(featureLocation.getAbsolutePath());
                        consumer.accept(
                                FileArtifactRepository.forFile(featureLocation,
                                        FeaturesAction.createFeatureArtifactKey(feature.getId(), feature.getVersion())),
                                FeaturesAction.createFeatureJarIU(feature, publisherInfo));
                    }
                    subMonitor.worked(1);
                }
            }
        }
    }

    private static void readBundles(File path, BiConsumer<IArtifactDescriptor, IInstallableUnit> consumer,
            IProgressMonitor monitor) throws ResolverException {
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                SubMonitor subMonitor = SubMonitor.convert(monitor, "reading bundles from path " + path + "...",
                        files.length);
                for (File bundleLocation : files) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String name = bundleLocation.getName();
                    if (name.startsWith(".") || bundleLocation.isFile() && !name.toLowerCase().endsWith(".jar")) {
                        continue;
                    }
                    subMonitor.subTask("Reading " + name);
                    try {
                        BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleLocation);
                        if (bundleDescription != null) {
                            IArtifactKey key = BundlesAction.createBundleArtifactKey(
                                    bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
                            consumer.accept(FileArtifactRepository.forFile(bundleLocation, key),
                                    BundlesAction.createBundleIU(bundleDescription, key, publisherInfo));
                        }
                    } catch (BundleException | IOException e) {
                        throw new ResolverException("Reading bundle failed", e);
                    }
                    subMonitor.worked(1);
                }
            }
        }
    }

}
