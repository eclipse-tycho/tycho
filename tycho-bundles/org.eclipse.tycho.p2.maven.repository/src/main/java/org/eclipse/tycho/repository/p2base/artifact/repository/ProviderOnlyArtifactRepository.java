/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

/**
 * Read-only repository which delegates artifact read operations to a provider instance. Adapter
 * from {@link IRawArtifactFileProvider} to {@link IFileArtifactRepository}.
 */
public class ProviderOnlyArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository,
        IRawArtifactFileProvider {

    private final IRawArtifactFileProvider delegate;

    // TODO only create via a factory -> class could be package private
    public ProviderOnlyArtifactRepository(IRawArtifactFileProvider delegate, IProvisioningAgent agent, URI location) {
        super(agent, null, ProviderOnlyArtifactRepository.class.getSimpleName(), "1.0", location,
                "Read-only repository adapter for " + delegate, null, null);
        this.delegate = delegate;
    }

    // delegated methods for canonical artifacts

    @Override
    public boolean contains(IArtifactKey key) {
        return delegate.contains(key);
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return delegate.query(query, monitor);
    }

    public File getArtifactFile(IArtifactKey key) {
        return delegate.getArtifactFile(key);
    }

    public IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        return delegate.getArtifact(key, destination, monitor);
    }

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        // letting the caller decide which format to use is non-sense
        return delegate.getArtifact(descriptor.getArtifactKey(), destination, monitor);
    }

    // delegated methods for raw artifacts

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        return delegate.getArtifactDescriptors(key);
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        return delegate.contains(descriptor);
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        return delegate.getArtifactFile(descriptor);
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        return delegate.getRawArtifact(descriptor, destination, monitor);
    }

    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        /*
         * Delegating this method would break the MirroringArtifactProvider: In order to determine
         * the IArtifactDescriptor, the artifact needs to be mirrored, so this method would trigger
         * a download of all remote artifacts. We could have this method return simplified
         * IArtifactDescriptors (e.g. without size, MD5 sums, etc.) but I'd rather not risk the
         * side-effects of this approach. AFAIK, we can get away with not supporting this operation.
         */
        throw new UnsupportedOperationException();
    }

    // default implementations

    // TODO shouldn't this be implemented in the super class from p2?
    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
        try {
            MultiStatus result = new MultiStatus(Activator.ID, 0, "Error while getting requested artifacts", null);
            for (IArtifactRequest request : requests) {
                request.perform(this, subMonitor.newChild(1));
                result.add(request.getResult());
            }
            if (!result.isOK()) {
                return result;
            } else {
                return Status.OK_STATUS;
            }
        } finally {
            monitor.done();
        }
    }

    // unsupported methods due to read-only

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDescriptors(IArtifactDescriptor[] descriptors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll(IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptor(IArtifactKey key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptors(IArtifactDescriptor[] descriptors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptors(IArtifactKey[] keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

}
