package org.eclipse.tycho.repository.p2base.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.p2base.artifact.provider.IArtifactProvider;

public abstract class AbstractArtifactRepository2 extends AbstractArtifactRepository implements IArtifactProvider {

    protected AbstractArtifactRepository2(IProvisioningAgent agent, String name, String type, String version,
            URI location, String description, String provider, Map<String, String> properties) {
        super(agent, name, type, version, location, description, provider, properties);
    }

    @Override
    public final IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        // letting the caller decide which format to use is non-sense
        return getArtifact(descriptor.getArtifactKey(), destination, monitor);
    }

    protected abstract void internalAddDescriptor(IArtifactDescriptor descriptor);

    protected abstract void internalStore(IProgressMonitor monitor);

    // methods that should really not be public; they violate the consistency of repositories
    @Deprecated
    @Override
    public final void addDescriptor(IArtifactDescriptor descriptor) {
        addDescriptor(descriptor, null);
    }

    @Deprecated
    @Override
    public final void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        internalAddDescriptor(descriptor);
        internalStore(monitor);
    }

    @Deprecated
    @Override
    public final void addDescriptors(IArtifactDescriptor[] descriptors) {
        addDescriptors(descriptors, null);
    }

    @Deprecated
    @Override
    public final void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        for (IArtifactDescriptor descriptor : descriptors) {
            internalAddDescriptor(descriptor);
        }
        internalStore(monitor);
    }

    // ensure that remove operations are properly implemented

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

    @Override
    public final void removeDescriptor(IArtifactKey key) {
        removeDescriptor(key, null);
    }

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

    // add default implementations missing in AbstractArtifactRepository

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

}
