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

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteCanonicalArtifactToSink;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteToSink;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactSink;

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
        AbstractArtifactRepository2 implements IFileArtifactRepository, IRawArtifactFileProvider {

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
     * This method is called by methods that add entries to the index, i.e. from
     * {@link #newAddingArtifactSink(IArtifactKey)}.
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

    // TODO deprecation message?
    @Deprecated
    @Override
    public final IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        IStatus status = getProcessedRawArtifact(descriptor, destination, monitor);

        setStatusOnStreamIfPossible(destination, status);
        return status;
    }

    public final IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);
        // method signature allows calls with a IRawArtifactSink -> make sure this is only done if sink requests a raw artifact in canonical format
        canWriteCanonicalArtifactToSink(sink);

        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        List<IArtifactDescriptor> formatsByPreference = transferPolicy
                .sortFormatsByPreference(getArtifactDescriptors(requestedKey));
        for (IArtifactDescriptor descriptor : formatsByPreference) {
            IStatus result;
            result = getProcessedRawArtifact(descriptor, sink.beginWrite(), monitor);
            // TODO only commit in case of success & try other formats -> no use case for LocalArtifactRepository
            sink.commitWrite();
            return result;
        }
        return errorStatus("Artifact " + requestedKey + " is not available in the repository " + getLocation(), null,
                ProvisionException.ARTIFACT_NOT_FOUND);
    }

    private IStatus getProcessedRawArtifact(IArtifactDescriptor descriptor, OutputStream destination,
            IProgressMonitor monitor) {

        OutputStream destinationWithProcessing = new ProcessingStepHandler().createAndLink(getProvisioningAgent(),
                descriptor.getProcessingSteps(), descriptor, destination, monitor);
        IStatus initStatus = ProcessingStepHandler.getStatus(destinationWithProcessing, true);
        if (isFatal(initStatus)) {
            return initStatus;
        }

        IStatus rawReadingStatus = getRawArtifact(descriptor, destinationWithProcessing, monitor);
        if (isFatal(rawReadingStatus)) {
            return rawReadingStatus;
        }

        try {
            closeProcessingSteps(destinationWithProcessing);
        } catch (IOException e) {
            return errorStatus("I/O exception while processing raw artifact " + descriptor, e);
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

    public final IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);

        IArtifactDescriptor descriptor = sink.getArtifactFormatToBeWritten();
        if (!contains(descriptor)) {
            return errorStatus("Artifact " + descriptor + " is not available in the repository " + getLocation(), null,
                    ProvisionException.ARTIFACT_NOT_FOUND);
        }

        OutputStream outputStream = sink.beginWrite();
        IStatus readStatus = readRawArtifact(descriptor, outputStream);
        // TODO only commit if successful
        sink.commitWrite();
        return readStatus;

    }

    // TODO deprecation message?
    @Deprecated
    public final IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination,
            IProgressMonitor monitor) {
        if (!contains(descriptor)) {
            IStatus status = errorStatus("Artifact " + descriptor + " is not available in the repository "
                    + getLocation(), null, ProvisionException.ARTIFACT_NOT_FOUND);
            setStatusOnStreamIfPossible(destination, status);
            return status;
        }

        IStatus status = readRawArtifact(descriptor, destination);
        setStatusOnStreamIfPossible(destination, status);
        return status;
    }

    private IStatus readRawArtifact(IArtifactDescriptor descriptor, OutputStream destination) {
        try {
            InputStream source = new FileInputStream(internalGetArtifactStorageLocation(descriptor));

            // copy to destination and close source
            FileUtils.copyStream(source, true, destination, false);

        } catch (IOException e) {
            return errorStatus("I/O exception while reading artifact " + descriptor, e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public final IArtifactSink newAddingArtifactSink(final IArtifactKey key) throws ProvisionException {
        ArtifactDescriptorT newDescriptor = getInternalDescriptorForAdding(createArtifactDescriptor(key));
        return internalNewAddingArtifactSink(newDescriptor);
    }

    protected final AddingArtifactSink internalNewAddingArtifactSink(ArtifactDescriptorT canonicalDescriptorToBeAdded)
            throws ProvisionException {
        // unlike RawAddingArtifactSink, this sink takes the artifact even if it needs to be converted from non-canonical format
        return new AddingArtifactSink(canonicalDescriptorToBeAdded);
    }

    @Override
    public final IRawArtifactSink newAddingRawArtifactSink(IArtifactDescriptor newDescriptor) throws ProvisionException {
        ArtifactDescriptorT newInternalDescriptorToBeAdded = getInternalDescriptorForAdding(newDescriptor);
        return new RawAddingArtifactSink(newInternalDescriptorToBeAdded);
    }

    private class AddingArtifactSink implements IArtifactSink {
        protected final ArtifactDescriptorT newDescriptor;
        private OutputStream currentOutputStream = null;
        private boolean committed = false;

        AddingArtifactSink(ArtifactDescriptorT newDescriptor) throws ProvisionException {
            if (contains(newDescriptor)) {
                IStatus status = errorStatus("Artifact " + newDescriptor + " already exists in repository "
                        + getLocation(), null, ProvisionException.ARTIFACT_EXISTS);
                throw new ProvisionException(status);
            }

            this.newDescriptor = newDescriptor;
        }

        public IArtifactKey getArtifactToBeWritten() {
            return newDescriptor.getArtifactKey();
        }

        public boolean canBeginWrite() {
            return !committed;
        }

        public OutputStream beginWrite() throws IllegalStateException, ArtifactSinkException {
            if (committed) {
                throw new IllegalStateException(
                        "This sink has already been used to add an artifact. Cannot start another write operation.");
            } else {
                // abort anything written so far
                abortWrite();
            }

            // TODO 397355 use a temporary file location in case multiple threads/processes write in parallel
            File artifactFile = internalGetArtifactStorageLocation(newDescriptor);
            artifactFile.getParentFile().mkdirs();

            try {
                currentOutputStream = new FileOutputStream(artifactFile);
            } catch (FileNotFoundException e) {
                throw new ArtifactSinkException("I/O error while creating artifact file " + artifactFile, e);
            }
            return currentOutputStream;
        }

        public void commitWrite() throws IllegalStateException, ArtifactSinkException {
            if (currentOutputStream == null) {
                throw new IllegalStateException("Write operation has not yet been started. Cannot add artifact.");
            }
            try {
                currentOutputStream.close();
            } catch (IOException e) {
                throw new ArtifactSinkException("I/O error while closing artifact file", e);
            } finally {
                currentOutputStream = null;
            }

            internalAddInternalDescriptor(newDescriptor);
            internalStore(null);
        }

        public void abortWrite() throws ArtifactSinkException {
            if (currentOutputStream == null) {
                return;
            }
            try {
                currentOutputStream.close();
            } catch (IOException e) {
                throw new ArtifactSinkException("I/O error while closing artifact file", e);
            } finally {
                currentOutputStream = null;
            }
        }
    }

    private class RawAddingArtifactSink extends AddingArtifactSink implements IRawArtifactSink {

        RawAddingArtifactSink(ArtifactDescriptorT newDescriptor) throws ProvisionException {
            super(newDescriptor);
        }

        public IArtifactDescriptor getArtifactFormatToBeWritten() {
            return newDescriptor;
        }

    }

    static IStatus errorStatus(String message, Throwable cause) {
        return errorStatus(message, cause, 0);
    }

    static IStatus errorStatus(String message, Throwable cause, int code) {
        return new Status(IStatus.ERROR, Activator.ID, code, message, cause);
    }
}
