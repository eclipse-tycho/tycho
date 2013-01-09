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
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

/**
 * Read-only repository which delegates artifact read operations to a provider instance. Adapter
 * from {@link IRawArtifactFileProvider} to {@link IFileArtifactRepository}.
 */
public class ProviderOnlyArtifactRepository extends AbstractArtifactRepository2 implements IFileArtifactRepository,
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

    // unsupported methods due to read-only

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    protected void internalAddDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalRemoveDescriptor(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalRemoveDescriptors(IArtifactDescriptor[] descriptors) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalRemoveDescriptors(IArtifactKey key) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalRemoveDescriptors(IArtifactKey[] keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalRemoveAllDescriptors() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void internalStore(IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        throw new UnsupportedOperationException();
    }

}
