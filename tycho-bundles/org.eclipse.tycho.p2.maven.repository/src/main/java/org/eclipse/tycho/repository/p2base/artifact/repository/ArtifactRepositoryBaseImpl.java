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

import static org.eclipse.tycho.repository.util.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.IArtifactFileProvider;

/**
 * Base implementation of a mutable, file system based p2 artifact repository. This class manages
 * changes to the index. Sub-classes need to
 * <ul>
 * <li>provide the storage location of artifacts &ndash; see
 * {@link #getArtifactFile(IArtifactDescriptor)}, and</li>
 * <li>provide the internal artifact descriptor instances &ndash; see
 * {@link #getInternalDescriptorForAdding(IArtifactDescriptor)},
 * {@link #getComparableDescriptor(IArtifactDescriptor)}</li>
 * </ul>
 */
@SuppressWarnings("restriction")
public abstract class ArtifactRepositoryBaseImpl<ArtifactDescriptorT extends IArtifactDescriptor> extends
        AbstractArtifactRepository2 implements IFileArtifactRepository, IArtifactFileProvider {

    private static final IArtifactDescriptor[] EMPTY_DESCRIPTOR_ARRAY = new IArtifactDescriptor[0];

    protected Set<ArtifactDescriptorT> descriptors = new HashSet<ArtifactDescriptorT>();
    protected Map<IArtifactKey, Set<ArtifactDescriptorT>> descriptorsMap = new HashMap<IArtifactKey, Set<ArtifactDescriptorT>>();

    private ArtifactTransferPolicy transferPolicy;

    protected ArtifactRepositoryBaseImpl(IProvisioningAgent agent, URI location, ArtifactTransferPolicy transferPolicy) {
        super(agent, null, null, null, location, null, null, null);
        this.transferPolicy = transferPolicy;
    }

    // index read access

    /**
     * Returns an {@link IArtifactDescriptor} instance which is comparable to the artifact
     * descriptors stored in the index (i.e. the {@link #descriptors} member). A valid
     * implementation is to convert the argument to the internal descriptor type
     * <code>ArtifactDescriptorT</code>, but this is not a requirement. This method should be
     * implemented in a way so that calling
     * <code>descriptors.contains(getComparableDescriptor(foreignDescriptor))</code> with a
     * descriptor from a foreign artifact repository returns <code>true</code> if and only if
     * copying that foreign artifact to this repository with
     * {@link #getOutputStream(IArtifactDescriptor)} would not add a new artifact to this
     * repository.
     * 
     * <p>
     * This method may be called by any API method with an {@link IArtifactDescriptor} argument.
     * </p>
     * 
     * @param descriptor
     *            An {@link IArtifactDescriptor} from any artifact repository.
     */
    protected abstract IArtifactDescriptor getComparableDescriptor(IArtifactDescriptor descriptor);

    @Override
    public final boolean contains(IArtifactKey key) {
        return descriptorsMap.containsKey(key);
    }

    @Override
    public final boolean contains(IArtifactDescriptor descriptor) {
        return descriptors.contains(getComparableDescriptor(descriptor));
    }

    @Override
    public final IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Set<ArtifactDescriptorT> descriptors = descriptorsMap.get(key);
        if (descriptors == null) {
            return EMPTY_DESCRIPTOR_ARRAY;
        }
        return descriptors.toArray(EMPTY_DESCRIPTOR_ARRAY);
    }

    public final IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        // TODO 397355 copy collection for thread-safety
        return query.perform(descriptorsMap.keySet().iterator());
    }

    @SuppressWarnings("unchecked")
    public final IQueryable<IArtifactDescriptor> descriptorQueryable() {
        // TODO 397355 copy collection for thread-safety
        return new IQueryable<IArtifactDescriptor>() {
            public IQueryResult<IArtifactDescriptor> query(IQuery<IArtifactDescriptor> query, IProgressMonitor monitor) {
                return query.perform((Iterator<IArtifactDescriptor>) descriptors.iterator());
            }
        };
    }

    // index write access

    /**
     * Returns an artifact descriptor of the internal descriptor type
     * <code>ArtifactDescriptorT</code> which may be added to the index. Implementations may require
     * that the descriptor argument is a descriptor instance created by this repository &ndash; see
     * {@link #createArtifactDescriptor(IArtifactKey)}.
     * 
     * <p>
     * This method is called for adding entries to the index, i.e. from
     * {@link #getOutputStream(IArtifactDescriptor)} (and the deprecated <code>addDescriptor</code>
     * methods).
     * </p>
     * 
     * @param descriptor
     *            An {@link IArtifactDescriptor} instance
     * @throws IllegalArgumentException
     *             if the provided descriptor cannot be added
     */
    protected abstract ArtifactDescriptorT getInternalDescriptorForAdding(IArtifactDescriptor descriptor)
            throws IllegalArgumentException;

    @Override
    protected final void internalAddDescriptor(IArtifactDescriptor descriptor) {
        internalAddInternalDescriptor(getInternalDescriptorForAdding(descriptor));
    }

    protected final void internalAddInternalDescriptor(ArtifactDescriptorT internalDescriptor) {
        descriptors.add(internalDescriptor);

        Set<ArtifactDescriptorT> descriptorsForKey = initDescriptorsMapEntry(internalDescriptor.getArtifactKey());
        descriptorsForKey.add(internalDescriptor);
    }

    private Set<ArtifactDescriptorT> initDescriptorsMapEntry(IArtifactKey key) {
        Set<ArtifactDescriptorT> mapEntry = descriptorsMap.get(key);

        if (mapEntry == null) {
            mapEntry = new HashSet<ArtifactDescriptorT>();
            descriptorsMap.put(key, mapEntry);
        }
        return mapEntry;
    }

    @Override
    protected final void internalRemoveDescriptor(IArtifactDescriptor descriptor) {
        IArtifactDescriptor comparableDescriptor = getComparableDescriptor(descriptor);
        descriptors.remove(comparableDescriptor);

        IArtifactKey artifactKey = comparableDescriptor.getArtifactKey();
        Set<ArtifactDescriptorT> descriptorsForKey = descriptorsMap.get(artifactKey);
        if (descriptorsForKey != null) {
            descriptorsForKey.remove(comparableDescriptor);

            if (descriptorsForKey.isEmpty()) {
                descriptorsMap.remove(artifactKey);
            }
        }
    }

    @Override
    protected final void internalRemoveDescriptors(IArtifactDescriptor[] descriptors) {
        for (IArtifactDescriptor descriptor : descriptors) {
            internalRemoveDescriptor(descriptor);
        }
    }

    @Override
    protected final void internalRemoveDescriptors(IArtifactKey key) {
        Set<ArtifactDescriptorT> descriptorsForKey = descriptorsMap.remove(key);
        if (descriptorsForKey != null) {
            for (ArtifactDescriptorT descriptor : descriptorsForKey) {
                descriptors.remove(descriptor);
            }
        }
    }

    @Override
    protected final void internalRemoveDescriptors(IArtifactKey[] keys) {
        for (IArtifactKey key : keys) {
            internalRemoveDescriptors(key);
        }
    }

    @Override
    protected final void internalRemoveAllDescriptors() {
        descriptors.clear();
        descriptorsMap.clear();
    }

    /**
     * Persists the index of this repository.
     * 
     * <p>
     * This method is called after every (bulk) change to the index.
     * </p>
     */
    @Override
    protected void internalStore(IProgressMonitor monitor) {
        // default: in memory so nothing to do
    }

    // artifact access

    /**
     * Returns the file system location where the given artifact is or would be stored. Unlike
     * {@link #getArtifactFile(IArtifactDescriptor)}, this method does not check if the given
     * artifact exists in the repository and never returns <code>null</code>.
     * 
     * <p>
     * This method may be called by any API method for reading or writing artifacts.
     * </p>
     */
    protected abstract File internalGetArtifactStorageLocation(IArtifactDescriptor descriptor);

    public final File getArtifactFile(IArtifactDescriptor descriptor) {
        if (contains(descriptor)) {
            return internalGetArtifactStorageLocation(descriptor);
        }
        return null;
    }

    public final File getArtifactFile(IArtifactKey key) {
        Set<ArtifactDescriptorT> descriptors = descriptorsMap.get(key);

        // if available, return location of canonical format of the artifact
        if (descriptors != null) {
            for (ArtifactDescriptorT descriptor : descriptors) {
                if (ArtifactTransferPolicy.isCanonicalFormat(descriptor)) {
                    return internalGetArtifactStorageLocation(descriptor);
                }
            }
        }
        return null;
    }

    // TODO handle error cases via exceptions; the IStatus return value makes it hard to extract methods
    public final IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        IArtifactDescriptor[] availableFormats = getArtifactDescriptors(key);
        if (availableFormats.length == 0) {
            return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + key
                    + " is not available in the repository " + getLocation(), null);
        }
        IArtifactDescriptor preferredFormat = transferPolicy.pickFormat(availableFormats);

        OutputStream destinationWithProcessing = new ProcessingStepHandler().createAndLink(getProvisioningAgent(),
                preferredFormat.getProcessingSteps(), preferredFormat, destination, monitor);
        IStatus initStatus = ProcessingStepHandler.getStatus(destinationWithProcessing, true);
        if (isFatal(initStatus)) {
            return initStatus;
        }

        IStatus rawReadingStatus = getRawArtifact(preferredFormat, destinationWithProcessing, monitor);
        if (isFatal(rawReadingStatus)) {
            return rawReadingStatus;
        }

        try {
            closeProcessingSteps(destinationWithProcessing);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, BUNDLE_ID, "I/O exception while processing raw artifact "
                    + preferredFormat);
        }

        IStatus processingStatus = ProcessingStepHandler.getStatus(destinationWithProcessing, true);
        return processingStatus;
    }

    private void closeProcessingSteps(OutputStream destinationWithProcessing) throws IOException {
        if (destinationWithProcessing instanceof ProcessingStep) {
            // close to flush content through processing steps and to trigger processing
            destinationWithProcessing.close();
        }
    }

    public final IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination,
            IProgressMonitor monitor) {
        if (!contains(descriptor)) {
            return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + descriptor
                    + " is not available in the repository " + getLocation(), null);
        }

        try {
            InputStream source = new FileInputStream(internalGetArtifactStorageLocation(descriptor));

            // copy to destination and close source
            FileUtils.copyStream(source, true, destination, false);

        } catch (IOException e) {
            return new Status(IStatus.ERROR, BUNDLE_ID, "I/O exception while reading artifact " + descriptor, e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public final OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        ArtifactDescriptorT internalDescriptor = getInternalDescriptorForAdding(descriptor);

        if (contains(internalDescriptor)) {
            IStatus status = new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_EXISTS, "Artifact "
                    + descriptor + " already exists in repository " + getLocation(), null);
            throw new ProvisionException(status);
        }

        // TODO 397355 use a temporary file location in case multiple threads/processes write in parallel
        File file = internalGetArtifactStorageLocation(internalDescriptor);

        try {
            return new CommittingArtifactOutputStream(file, internalDescriptor);
        } catch (FileNotFoundException e) {
            // TODO revise message?
            throw new ProvisionException("Could not create artifact file", e);
        }
    }

    private class CommittingArtifactOutputStream extends OutputStream implements IStateful {
        final FileOutputStream artifactSink;

        private ArtifactDescriptorT artifactDescriptorToAdd;
        private IStatus externallySetStatus = Status.OK_STATUS;

        CommittingArtifactOutputStream(File artifactLocation, ArtifactDescriptorT artifactDescriptorToAdd)
                throws FileNotFoundException {
            artifactLocation.getParentFile().mkdirs();
            this.artifactSink = new FileOutputStream(artifactLocation);

            this.artifactDescriptorToAdd = artifactDescriptorToAdd;
        }

        public void setStatus(IStatus status) {
            if (status == null) {
                throw new NullPointerException();
            }
            externallySetStatus = status;
        }

        public IStatus getStatus() {
            return externallySetStatus;
        }

        @Override
        public void close() throws IOException {
            artifactSink.close();

            if (!isFatal(externallySetStatus)) {
                internalAddInternalDescriptor(artifactDescriptorToAdd);
                internalStore(null);
            }
        }

        @Override
        public void write(int b) throws IOException {
            artifactSink.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            artifactSink.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            artifactSink.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            artifactSink.flush();
        }

    }

    static boolean isFatal(IStatus status) {
        return status.matches(IStatus.ERROR | IStatus.CANCEL);
    }

}
