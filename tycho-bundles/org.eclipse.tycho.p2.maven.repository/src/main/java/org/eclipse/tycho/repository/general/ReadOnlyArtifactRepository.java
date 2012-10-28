/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.general;

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
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.Activator;

public class ReadOnlyArtifactRepository extends AbstractArtifactRepository implements IFileArtifactRepository,
        IRawArtifactFileProvider {

    private final IRawArtifactFileProvider delegate;

    public static ReadOnlyArtifactRepository adapt(IRawArtifactFileProvider delegate, IProvisioningAgent agent,
            URI location) {
        // TODO 393004 what happens if the location is different?
//        if (delegate instanceof FileArtifactRepositoryAdapter) {
//            return (FileArtifactRepositoryAdapter) delegate;
//        }

        return new ReadOnlyArtifactRepository(delegate, agent, location);
    }

    private ReadOnlyArtifactRepository(IRawArtifactFileProvider delegate, IProvisioningAgent agent, URI location) {
        super(agent, location.toString(), ReadOnlyArtifactRepository.class.getSimpleName(), "1.0", location,
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
         * Delegating this method would break the CachingArtifactProvider: In order to determine the
         * IArtifactDescriptor, the artifact in general needs to be available locally. Hence this
         * method would trigger a download of all remote artifacts. We could have this method return
         * simplified IArtifactDescriptors (e.g. without size, MD5 sums, etc.) but I'd rather not
         * risk the side-effects of this approach. AFAIK, we can get away with not supporting this
         * operation.
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

    public boolean isModifiable() {
        return false;
    }

    public String setProperty(String key, String value) {
        throw new UnsupportedOperationException();
    }

    public String setProperty(String key, String value, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
        throw new UnsupportedOperationException();
    }

    public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
        throw new UnsupportedOperationException();
    }

    public void addDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public void addDescriptors(IArtifactDescriptor[] descriptors) {
        throw new UnsupportedOperationException();
    }

    public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    public void removeAll(IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptor(IArtifactKey key) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptors(IArtifactDescriptor[] descriptors) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptors(IArtifactKey[] keys) {
        throw new UnsupportedOperationException();
    }

    public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

}
