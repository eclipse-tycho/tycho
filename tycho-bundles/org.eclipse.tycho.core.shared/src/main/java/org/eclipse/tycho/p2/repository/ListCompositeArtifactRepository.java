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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.osgi.util.NLS;

/**
 * A {@link ICompositeRepository}/ {@link IArtifactRepository} that is backed by a simple list, in
 * contrast to the default P2 this does not require any access to the repository manager and simply
 * aggregates all data, besides this, it also implements {@link IFileArtifactRepository} on top
 *
 */
public class ListCompositeArtifactRepository extends AbstractArtifactRepository
        implements ICompositeRepository<IArtifactKey>, IFileArtifactRepository {

    public final List<IArtifactRepository> artifactRepositories;

    public ListCompositeArtifactRepository(IProvisioningAgent agent,
            List<? extends IArtifactRepository> artifactRepositories) {
        super(agent, null, IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null, null, null, null, null);
        try {
            setLocation(new URI("list:" + UUID.randomUUID()));
        } catch (URISyntaxException e) {
            throw new AssertionError("should never happen", e);
        }
        this.artifactRepositories = List.copyOf(artifactRepositories);
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        int size = artifactRepositories.size();
        if (size == 1) {
            return artifactRepositories.get(0).query(query, monitor);
        }
        Collector<IArtifactKey> collector = new Collector<>();
        SubMonitor subMonitor = SubMonitor.convert(monitor, size);
        for (IArtifactRepository repository : artifactRepositories) {
            collector.addAll(repository.query(query, subMonitor.split(1)));
        }
        return collector;
    }

    @Override
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        int size = artifactRepositories.size();
        if (size == 1) {
            return artifactRepositories.get(0).getRawArtifact(descriptor, destination, monitor);
        }
        SubMonitor subMonitor = SubMonitor.convert(monitor);
        for (int i = 0; i < size; i++) {
            if (subMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            IArtifactRepository artifactRepository = artifactRepositories.get(i);
            if (artifactRepository.contains(descriptor)) {
                return artifactRepository.getRawArtifact(descriptor, destination, subMonitor);
            }
        }
        return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, descriptor));
    }

    @Override
    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        if (artifactRepositories.size() == 1) {
            return artifactRepositories.get(0).descriptorQueryable();
        }
        return (query, monitor) -> {
            SubMonitor subMonitor = SubMonitor.convert(monitor, artifactRepositories.size());
            Collector<IArtifactDescriptor> collector = new Collector<>();
            for (IArtifactRepository repository : artifactRepositories) {
                collector.addAll(repository.descriptorQueryable().query(query, subMonitor.split(1)));
            }
            return collector;
        };
    }

    @Override
    public void addChild(URI child) {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<URI> getChildren() {
        List<URI> list = new ArrayList<>();
        for (IArtifactRepository repository : artifactRepositories) {
            list.add(repository.getLocation());
        }
        return list;
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeChild(URI child) {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        for (IArtifactRepository repository : artifactRepositories) {
            if (repository.contains(descriptor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(IArtifactKey key) {
        for (IArtifactRepository repository : artifactRepositories) {
            if (repository.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        for (IArtifactRepository repository : artifactRepositories) {
            if (repository.contains(descriptor)) {
                return repository.getArtifact(descriptor, destination, monitor);
            }
        }
        return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, descriptor));
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        List<IArtifactDescriptor> result = new ArrayList<>();
        for (IArtifactRepository repository : artifactRepositories) {
            if (repository.contains(key)) {
                IArtifactDescriptor[] tempResult = repository.getArtifactDescriptors(key);
                result.addAll(Arrays.asList(tempResult));
            }
        }
        return result.toArray(new IArtifactDescriptor[result.size()]);

    }

    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        int size = artifactRepositories.size();
        if (size == 1) {
            return artifactRepositories.get(0).getArtifacts(requests, monitor);
        }
        MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_artifactsFromChildRepos,
                null);
        SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length * size);
        for (int i = 0; i < requests.length; i++) {
            if (subMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            IArtifactRequest request = requests[i];
            subMonitor.setWorkRemaining((requests.length - i) * size);
            IArtifactDescriptor[] descriptors = getArtifactDescriptors(request.getArtifactKey());
            if (descriptors.length > 0 && descriptors[0].getRepository() != null) {
                multiStatus.add(descriptors[0].getRepository().getArtifacts(new IArtifactRequest[] { request },
                        subMonitor.split(1)));
            } else {
                for (IArtifactRepository repository : artifactRepositories) {
                    if (repository.contains(request.getArtifactKey())) {
                        IStatus status = repository.getArtifacts(new IArtifactRequest[] { request },
                                subMonitor.split(1));
                        multiStatus.add(status);
                        if (status.getSeverity() == IStatus.CANCEL) {
                            return multiStatus;
                        }
                        if (status.isOK()) {
                            return status;
                        }
                        // else: something is fishy (eg inconsistent artifact metadata
                        // across multiple repo: same GAV, different hash), and a warning
                        // should be emitted
                    }
                }
            }
        }
        return multiStatus;
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getArtifactFile(IArtifactKey key) {
        return getFileFromRepositories(key, IFileArtifactRepository::contains,
                IFileArtifactRepository::getArtifactFile);
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return getFileFromRepositories(descriptor, IFileArtifactRepository::contains,
                IFileArtifactRepository::getArtifactFile);
    }

    private <X> File getFileFromRepositories(X id, BiPredicate<IFileArtifactRepository, X> when,
            BiFunction<IFileArtifactRepository, X, File> function) {
        return artifactRepositories.stream().filter(IFileArtifactRepository.class::isInstance)
                .map(IFileArtifactRepository.class::cast).filter(r -> when.test(r, id)).map(r -> function.apply(r, id))
                .filter(f -> f != null).findFirst().orElse(null);
    }

}
