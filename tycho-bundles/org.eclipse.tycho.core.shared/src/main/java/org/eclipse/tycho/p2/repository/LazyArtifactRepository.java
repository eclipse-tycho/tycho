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
package org.eclipse.tycho.p2.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;

public class LazyArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

    private BiFunction<URI, IProvisioningAgent, IArtifactRepository> loader;

    private IArtifactRepository delegate;

    public LazyArtifactRepository(IProvisioningAgent agent, URI uri,
            BiFunction<URI, IProvisioningAgent, IArtifactRepository> loader) {
        super(agent, "LazyArtifactRepository(" + uri + ")", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null,
                uri, null, null, null);
        this.loader = loader;
    }

    private synchronized IArtifactRepository getDelegate() {
        if (delegate == null) {
            delegate = loader.apply(getLocation(), getProvisioningAgent());
            if (delegate == null) {
                throw new RuntimeException("lazy loading repository from location " + getLocation() + " failed");
            }
            setName(delegate.getName());
            setDescription(delegate.getDescription());
            setVersion(delegate.getVersion());
            setProperties(delegate.getProperties());
            setProvider(delegate.getProvider());
            setType(delegate.getType());
            setVersion(delegate.getVersion());
        }
        return delegate;
    }

    @Override
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return getDelegate().getRawArtifact(descriptor, destination, monitor);
    }

    @Override
    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return getDelegate().descriptorQueryable();
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return getDelegate().query(query, monitor);
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        return getDelegate().contains(descriptor);
    }

    @Override
    public boolean contains(IArtifactKey key) {
        return getDelegate().contains(key);
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return getDelegate().getArtifact(descriptor, destination, monitor);
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        return getDelegate().getArtifactDescriptors(key);
    }

    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        return getDelegate().getArtifacts(requests, monitor);
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        return getDelegate().getOutputStream(descriptor);
    }

    @Override
    public File getArtifactFile(IArtifactKey key) {
        return getDelegate() instanceof IFileArtifactRepository fileArtifactRepo //
                ? fileArtifactRepo.getArtifactFile(key)
                : null;
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return getDelegate() instanceof IFileArtifactRepository fileArtifactRepo
                ? fileArtifactRepo.getArtifactFile(descriptor)
                : null;
    }

}
