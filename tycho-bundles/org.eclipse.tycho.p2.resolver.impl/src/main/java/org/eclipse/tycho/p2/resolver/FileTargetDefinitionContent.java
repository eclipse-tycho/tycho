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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.p2.target.TargetDefinitionContent;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.util.resolution.ResolverException;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class FileTargetDefinitionContent implements TargetDefinitionContent {

    private Map<IArtifactDescriptor, IInstallableUnit> repositoryContent = new HashMap<IArtifactDescriptor, IInstallableUnit>();
    private final FileMetadataRepository metadataRepository;
    private final FileArtifactRepository artifactRepository;
    private File location;

    private boolean loaded;

    public FileTargetDefinitionContent(IProvisioningAgent agent, File location) {
        this.location = location;
        metadataRepository = new FileMetadataRepository(location, agent,
                () -> getRepositoryContent().values().iterator());
        artifactRepository = new FileArtifactRepository(location, agent,
                () -> getRepositoryContent().keySet().iterator());
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
                                new FileArtifactDescriptor(featureLocation,
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
                            consumer.accept(new FileArtifactDescriptor(bundleLocation, key),
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

    private static boolean matches(IArtifactKey key, IArtifactDescriptor descriptor) {
        IArtifactKey descriptorKey = descriptor.getArtifactKey();
        return descriptorKey == key || (key.getId().equals(descriptorKey.getId())
                && key.getClassifier().equals(descriptorKey.getClassifier())
                && key.getVersion().equals(descriptorKey.getVersion()));
    }

    private static final class FileArtifactDescriptor extends ArtifactDescriptor {

        private File file;

        private FileArtifactDescriptor(File file, IArtifactKey key) {
            super(key);
            this.file = file;
        }

    }

    private static class FileArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

        private Supplier<Iterator<IArtifactDescriptor>> descriptorSupplier;

        protected FileArtifactRepository(File location, IProvisioningAgent agent,
                Supplier<Iterator<IArtifactDescriptor>> descriptorSupplier) {
            super(agent, location.getName(), null, null, location.toURI(), null, null, null);
            this.descriptorSupplier = descriptorSupplier;
        }

        @Override
        public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination,
                IProgressMonitor monitor) {
            File artifactFile = getArtifactFile(descriptor);
            if (artifactFile == null) {
                return new Status(IStatus.ERROR, FileTargetDefinitionContent.class.getName(), "Artifact not found");
            }
            try {
                try (FileInputStream inputStream = new FileInputStream(artifactFile)) {
                    inputStream.transferTo(destination);
                }
            } catch (IOException e) {
                return new Status(IStatus.ERROR, FileTargetDefinitionContent.class.getName(), "transfer failed", e);
            }
            return Status.OK_STATUS;
        }

        @Override
        public IQueryable<IArtifactDescriptor> descriptorQueryable() {
            return (query, monitor) -> query.perform(descriptorSupplier.get());
        }

        @Override
        public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
            Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
            return query.perform(new Iterator<IArtifactKey>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public IArtifactKey next() {
                    IArtifactDescriptor next = iterator.next();
                    return next.getArtifactKey();
                }
            });
        }

        @Override
        public boolean contains(IArtifactDescriptor descriptor) {
            Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
            while (iterator.hasNext()) {
                IArtifactDescriptor thisArtifactDescriptor = (IArtifactDescriptor) iterator.next();
                if (thisArtifactDescriptor.equals(descriptor)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(IArtifactKey key) {
            Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
            while (iterator.hasNext()) {
                IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
                if (matches(key, descriptor)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
            return getRawArtifact(descriptor, destination, monitor);
        }

        @Override
        public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
            Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
            while (iterator.hasNext()) {
                IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
                if (matches(key, descriptor)) {
                    return new IArtifactDescriptor[] { descriptor };
                }
            }
            return new IArtifactDescriptor[0];
        }

        @Override
        public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
            SubMonitor convert = SubMonitor.convert(monitor, requests.length);
            MultiStatus multiStatus = new MultiStatus(FileTargetDefinitionContent.class.getName(), IStatus.INFO,
                    "Request Status");
            boolean ok = true;
            for (IArtifactRequest request : requests) {
                request.perform(this, convert.split(1));
                IStatus result = request.getResult();
                multiStatus.add(result);
                ok &= result.isOK();
            }
            return ok ? Status.OK_STATUS : multiStatus;
        }

        @Override
        public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
            throw new ProvisionException("read only");
        }

        @Override
        public File getArtifactFile(IArtifactKey key) {
            Iterator<IArtifactDescriptor> iterator = descriptorSupplier.get();
            while (iterator.hasNext()) {
                IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
                if (matches(key, descriptor)) {
                    return getArtifactFile(descriptor);
                }
            }
            return null;
        }

        @Override
        public File getArtifactFile(IArtifactDescriptor descriptor) {
            if (descriptor instanceof FileArtifactDescriptor) {
                return ((FileArtifactDescriptor) descriptor).file;
            }
            return null;
        }

    }

    private static class FileMetadataRepository extends AbstractMetadataRepository {

        private Supplier<Iterator<IInstallableUnit>> unitprovider;

        public FileMetadataRepository(File location, IProvisioningAgent agent,
                Supplier<Iterator<IInstallableUnit>> unitprovider) {
            super(agent);
            this.unitprovider = unitprovider;
            setLocation(location.toURI());
        }

        @Override
        public Collection<IRepositoryReference> getReferences() {
            return Collections.emptyList();
        }

        @Override
        public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
            return query.perform(unitprovider.get());
        }

        @Override
        public void initialize(RepositoryState state) {

        }

    }
}
