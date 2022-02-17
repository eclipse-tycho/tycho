/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *    Christoph Läubrich - Issue #658 - Tycho strips p2 artifact properties (eg PGP, maven info...) 
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkFactory.rawWriteToStream;
import static org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkFactory.writeToStream;
import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

/**
 * Base class for {@link IArtifactRepository} implementations that fixes some problems in the p2 API
 * and p2's base class {@link AbstractArtifactRepository}.
 */
public abstract class AbstractArtifactRepository2 extends AbstractArtifactRepository implements IRawArtifactProvider {

    protected AbstractArtifactRepository2(IProvisioningAgent agent, String name, String type, String version,
            URI location, String description, String provider, Map<String, String> properties) {
        super(agent, name, type, version, location, description, provider, properties);
    }

    // deprecate methods that should really not be public

    protected abstract void internalAddDescriptor(IArtifactDescriptor descriptor);

    protected abstract void internalStore(IProgressMonitor monitor);

    /**
     * {@inheritDoc}
     * 
     * @deprecated This method requires assumptions about the internal artifact layout and may bring
     *             the repository into an inconsistent state. Use
     *             {@link #newAddingArtifactSink(IArtifactKey)} or
     *             {@link #newAddingRawArtifactSink(IArtifactDescriptor)} instead.
     */
    @Deprecated
    @Override
    public final void addDescriptor(IArtifactDescriptor descriptor) {
        addDescriptor(descriptor, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This method requires assumptions about the internal artifact layout and may bring
     *             the repository into an inconsistent state. Use
     *             {@link #newAddingArtifactSink(IArtifactKey)} or
     *             {@link #newAddingRawArtifactSink(IArtifactDescriptor)} instead.
     */
    @Deprecated
    @Override
    public final void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        internalAddDescriptor(descriptor);
        internalStore(monitor);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This method requires assumptions about the internal artifact layout and may bring
     *             the repository into an inconsistent state. Use
     *             {@link #newAddingArtifactSink(IArtifactKey)} or
     *             {@link #newAddingRawArtifactSink(IArtifactDescriptor)} instead.
     */
    @Deprecated
    @Override
    public final void addDescriptors(IArtifactDescriptor[] descriptors) {
        addDescriptors(descriptors, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This method requires assumptions about the internal artifact layout and may bring
     *             the repository into an inconsistent state. Use
     *             {@link #newAddingArtifactSink(IArtifactKey)} or
     *             {@link #newAddingRawArtifactSink(IArtifactDescriptor)} instead.
     */
    @Deprecated
    @Override
    public final void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        for (IArtifactDescriptor descriptor : descriptors) {
            internalAddDescriptor(descriptor);
        }
        internalStore(monitor);
    }

    // enforce that remove operations are implemented; don't inherit empty (=broken) implementations from p2's AbstractArtifactRepository

    protected abstract void internalRemoveDescriptor(IArtifactDescriptor descriptor);

    protected abstract void internalRemoveDescriptors(IArtifactDescriptor[] descriptors);

    protected abstract void internalRemoveDescriptors(IArtifactKey key);

    protected abstract void internalRemoveDescriptors(IArtifactKey[] keys);

    protected abstract void internalRemoveAllDescriptors();

    @Override
    public final void removeDescriptor(IArtifactDescriptor descriptor) {
        removeDescriptor(descriptor, null);
    }

    @Override
    public final void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        internalRemoveDescriptor(descriptor);

        // TODO only store if something was removed?
        internalStore(monitor);
    }

    @Override
    public final void removeDescriptors(IArtifactDescriptor[] descriptors) {
        removeDescriptors(descriptors, null);
    }

    @Override
    public final void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        internalRemoveDescriptors(descriptors);
        internalStore(monitor);
    }

    // should be called removeDescriptors
    @Override
    public final void removeDescriptor(IArtifactKey key) {
        removeDescriptor(key, null);
    }

    // should be called removeDescriptors
    @Override
    public final void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
        internalRemoveDescriptors(key);
        internalStore(monitor);
    }

    @Override
    public final void removeDescriptors(IArtifactKey[] keys) {
        removeDescriptors(keys, null);
    }

    @Override
    public final void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
        internalRemoveDescriptors(keys);
        internalStore(monitor);
    }

    @Override
    public final void removeAll() {
        removeAll(null);
    }

    @Override
    public final void removeAll(IProgressMonitor monitor) {
        internalRemoveAllDescriptors();
        internalStore(monitor);
    }

    // replace API methods for writing artifacts (see bug 400442)

    /**
     * Returns a new {@link IArtifactSink} instance that adds the received artifact to this
     * repository on {@link IArtifactSink#commitWrite()}.
     * 
     * @param key
     *            The artifact key to be added to this repository
     * @throws ProvisionException
     *             if the artifact is already stored in the repository (in canonical format)
     */
    // TODO this method should be exposed via an interface
    public abstract IArtifactSink newAddingArtifactSink(final IArtifactDescriptor descriptor) throws ProvisionException;

    /**
     * Returns a new {@link IRawArtifactSink} instance that adds the received artifact to this
     * repository on {@link IArtifactSink#commitWrite()}.
     * 
     * @param descriptor
     *            The artifact descriptor to the added to this repository
     * @throws ProvisionException
     *             if the artifact is already stored in the repository in the given format.
     */
    // TODO this method should be exposed via an interface
    public abstract IRawArtifactSink newAddingRawArtifactSink(IArtifactDescriptor descriptor) throws ProvisionException;

    /**
     * {@inheritDoc}
     * 
     * @deprecated The commit logic of this method is undocumented and brittle (bug 400442). Use
     *             {@link #newAddingArtifactSink(IArtifactKey)} or
     *             {@link #newAddingRawArtifactSink(IArtifactDescriptor)} instead.
     */
    @Deprecated
    @Override
    public final OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        try {
            IArtifactSink artifactSink = newAddingRawArtifactSink(descriptor);
            return new CommittingArtifactOutputStream(artifactSink);
        } catch (ArtifactSinkException e) {
            throw new ProvisionException(
                    new Status(IStatus.ERROR, BUNDLE_ID, "Error while writing to artifact sink: " + e.getMessage(), e));
        }
    }

    private class CommittingArtifactOutputStream extends OutputStream implements IStateful {
        final IArtifactSink artifactSink;
        final OutputStream artifactOutputStream;

        private IStatus externallySetStatus = Status.OK_STATUS;

        public CommittingArtifactOutputStream(IArtifactSink artifactSink) throws ArtifactSinkException {
            this.artifactSink = artifactSink;
            this.artifactOutputStream = artifactSink.beginWrite();
        }

        @Override
        public void setStatus(IStatus status) {
            if (status == null) {
                throw new NullPointerException();
            }
            externallySetStatus = status;
        }

        @Override
        public IStatus getStatus() {
            return externallySetStatus;
        }

        @Override
        public void close() throws IOException {
            artifactOutputStream.close();

            try {
                if (isFatal(externallySetStatus)) {
                    artifactSink.abortWrite();
                } else {
                    artifactSink.commitWrite();
                }
            } catch (ArtifactSinkException e) {
                // hard to to do anything better here - IOException doesn't take a cause
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(int b) throws IOException {
            artifactOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            artifactOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            artifactOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            artifactOutputStream.flush();
        }

    }

    // implementations for old get(Raw)Artifact methods

    /**
     * {@inheritDoc}
     * 
     * @deprecated Obsolete. Use {@link #getArtifact(IArtifactSink, IProgressMonitor)} instead.
     */
    // TODO make final?
    @Override
    @Deprecated
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        try {
            // use any format for getting the canonical artifact
            IArtifactKey requestedKey = descriptor.getArtifactKey();

            IStatus status = getArtifact(writeToStream(requestedKey, destination), monitor);
            setStatusOnStreamIfPossible(destination, status);
            return status;

        } catch (ArtifactSinkException e) {
            // the sink used shouldn't throw this exception 
            return new Status(IStatus.ERROR, BUNDLE_ID, e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated Obsolete. Use {@link #getRawArtifact(IRawArtifactSink, IProgressMonitor)}
     *             instead.
     */
    // TODO make final?
    @Override
    @Deprecated
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        try {
            IStatus status = getRawArtifact(rawWriteToStream(descriptor, destination), monitor);
            setStatusOnStreamIfPossible(destination, status);
            return status;

        } catch (ArtifactSinkException e) {
            // the sink used shouldn't throw this exception 
            return new Status(IStatus.ERROR, BUNDLE_ID, e.getMessage(), e);
        }
    }

    // add default implementations missing in AbstractArtifactRepository

    @Override
    public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
        try {
            MultiStatus result = new MultiStatus(BUNDLE_ID, 0, "Error while getting requested artifacts", null);
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

    protected static boolean isFatal(IStatus status) {
        return status.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    protected static void setStatusOnStreamIfPossible(OutputStream destination, IStatus status) {
        if (destination instanceof IStateful) {
            ((IStateful) destination).setStatus(status);
        }
    }

}
