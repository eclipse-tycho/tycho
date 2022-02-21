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

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteCanonicalArtifactToSink;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteToSink;
import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

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
public abstract class ArtifactRepositoryBaseImpl<ArtifactDescriptorT extends IArtifactDescriptor>
        extends AbstractArtifactRepository2 implements IFileArtifactRepository, IRawArtifactFileProvider {

    private static final IArtifactDescriptor[] EMPTY_DESCRIPTOR_ARRAY = new IArtifactDescriptor[0];

    protected Map<IArtifactKey, Set<ArtifactDescriptorT>> descriptorsMap = new ConcurrentHashMap<>();

    private ArtifactTransferPolicy transferPolicy;

    protected ArtifactRepositoryBaseImpl(IProvisioningAgent agent, URI location,
            ArtifactTransferPolicy transferPolicy) {
        super(agent, null, null, null, location, null, null, null);
        this.transferPolicy = transferPolicy;
    }

    // index read access

    /**
     * Returns an {@link IArtifactDescriptor} instance which is comparable to the artifact
     * descriptors stored in the index (i.e. the {@link #descriptorsMap} member). A valid
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
        IArtifactDescriptor comparableDescriptor = getComparableDescriptor(descriptor);
        return descriptorsMap.values().stream().anyMatch(set -> set.contains(comparableDescriptor));
    }

    @Override
    public final IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Set<ArtifactDescriptorT> descriptors = descriptorsMap.get(key);
        if (descriptors == null) {
            return EMPTY_DESCRIPTOR_ARRAY;
        }
        return descriptors.toArray(EMPTY_DESCRIPTOR_ARRAY);
    }

    @Override
    public final IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return query.perform(descriptorsMap.keySet().iterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return (query, monitor) -> query.perform((Iterator<IArtifactDescriptor>) flattenedValues().iterator());
    }

    protected final Stream<ArtifactDescriptorT> flattenedValues() {
        return descriptorsMap.values().stream().flatMap(Collection::stream);
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
        Set<ArtifactDescriptorT> descriptorsForKey = descriptorsMap.computeIfAbsent(internalDescriptor.getArtifactKey(),
                k -> ConcurrentHashMap.newKeySet());
        descriptorsForKey.add(internalDescriptor);
    }

    @Override
    protected final void internalRemoveDescriptor(IArtifactDescriptor descriptor) {
        IArtifactDescriptor comparableDescriptor = getComparableDescriptor(descriptor);

        IArtifactKey artifactKey = comparableDescriptor.getArtifactKey();
        descriptorsMap.computeIfPresent(artifactKey, (k, descriptors) -> {
            descriptors.remove(comparableDescriptor);
            return descriptors.isEmpty() ? null : descriptors;
        });
    }

    @Override
    protected final void internalRemoveDescriptors(IArtifactDescriptor[] descriptors) {
        for (IArtifactDescriptor descriptor : descriptors) {
            internalRemoveDescriptor(descriptor);
        }
    }

    @Override
    protected void internalRemoveDescriptors(IArtifactKey key) {
        descriptorsMap.remove(key);
    }

    @Override
    protected final void internalRemoveDescriptors(IArtifactKey[] keys) {
        for (IArtifactKey key : keys) {
            internalRemoveDescriptors(key);
        }
    }

    @Override
    protected final void internalRemoveAllDescriptors() {
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

    @Override
    public final File getArtifactFile(IArtifactDescriptor descriptor) {
        if (contains(descriptor)) {
            return internalGetArtifactStorageLocation(descriptor);
        }
        return null;
    }

    @Override
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

    /**
     * {@inheritDoc}
     * 
     * @deprecated Obsolete. Use {@link #getArtifact(IArtifactSink, IProgressMonitor)} instead.
     */
    @Deprecated
    @Override
    public final IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination,
            IProgressMonitor monitor) {
        /*
         * TODO remove this method?
         * 
         * The difference of this implementation to overridden implementation is that here, the
         * internal format for getting the artifact (in canonical format) can be selected by the
         * caller. AFAIK, the only use case for this is to implement the retry logic on the outside
         * of IArtifactRepository (see p2's MirrorRequest). With the IArtifactSink-based getArtifact
         * method, there is a much better alternative.
         */
        IStatus status;
        if (!contains(descriptor)) {
            status = errorStatus("Artifact " + descriptor + " is not available in the repository " + getLocation(),
                    null, ProvisionException.ARTIFACT_NOT_FOUND);
        } else {
            status = getProcessedRawArtifact(descriptor, destination, monitor);
        }

        setStatusOnStreamIfPossible(destination, status);
        return status;
    }

    @Override
    public final IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);
        // method signature allows calls with a IRawArtifactSink -> make sure this is only done if sink requests a raw artifact in canonical format
        canWriteCanonicalArtifactToSink(sink);

        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        IArtifactDescriptor[] availableFormats = getArtifactDescriptors(requestedKey);
        List<IArtifactDescriptor> formatsByPreference = transferPolicy.sortFormatsByPreference(availableFormats);

        for (IArtifactDescriptor descriptor : formatsByPreference) {
            IStatus result = getProcessedRawArtifact(descriptor, sink.beginWrite(), monitor);

            // trying other formats is no use case for the LocalArtifactRepository - if the preferred format (=canonical) is corrupt, it can fail straight away
            // TODO implement retry for other implementations?
            closeSinkAccordingToStatus(sink, result);
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

        IStatus rawReadingStatus = readRawArtifact(descriptor, destinationWithProcessing);
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

    @Override
    public final IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);

        IArtifactDescriptor descriptor = sink.getArtifactFormatToBeWritten();
        if (!contains(descriptor)) {
            return errorStatus("Artifact " + descriptor + " is not available in the repository " + getLocation(), null,
                    ProvisionException.ARTIFACT_NOT_FOUND);
        }

        IStatus status = readRawArtifact(descriptor, sink.beginWrite());
        closeSinkAccordingToStatus(sink, status);
        return status;
    }

    private IStatus readRawArtifact(IArtifactDescriptor descriptor, OutputStream destination) {
        try (InputStream source = new FileInputStream(internalGetArtifactStorageLocation(descriptor))) {
            // copy to destination and close source
            source.transferTo(destination);

        } catch (IOException e) {
            return errorStatus("I/O exception while reading artifact " + descriptor, e);
        }
        return Status.OK_STATUS;
    }

    private static void closeSinkAccordingToStatus(IArtifactSink sink, IStatus status) throws ArtifactSinkException {
        if (isFatal(status)) {
            sink.abortWrite();
        } else {
            sink.commitWrite();
        }
    }

    @Override
    public final IArtifactSink newAddingArtifactSink(final IArtifactDescriptor descriptor) throws ProvisionException {
        ArtifactDescriptorT newDescriptor = getInternalDescriptorForAdding(descriptor);
        return internalNewAddingArtifactSink(newDescriptor);
    }

    protected final AddingArtifactSink internalNewAddingArtifactSink(ArtifactDescriptorT canonicalDescriptorToBeAdded)
            throws ProvisionException {
        // unlike RawAddingArtifactSink, this sink takes the artifact even if it needs to be converted from non-canonical format
        return new AddingArtifactSink(canonicalDescriptorToBeAdded);
    }

    @Override
    public final IRawArtifactSink newAddingRawArtifactSink(IArtifactDescriptor newDescriptor)
            throws ProvisionException {
        ArtifactDescriptorT newInternalDescriptorToBeAdded = getInternalDescriptorForAdding(newDescriptor);
        return new RawAddingArtifactSink(newInternalDescriptorToBeAdded);
    }

    private class AddingArtifactSink implements IArtifactSink {
        protected final ArtifactDescriptorT newDescriptor;
        private OutputStream currentOutputStream = null;
        private boolean committed = false;

        AddingArtifactSink(ArtifactDescriptorT newDescriptor) throws ProvisionException {
            if (contains(newDescriptor)) {
                IStatus status = errorStatus(
                        "Artifact " + newDescriptor + " already exists in repository " + getLocation(), null,
                        ProvisionException.ARTIFACT_EXISTS);
                throw new ProvisionException(status);
            }

            this.newDescriptor = newDescriptor;
        }

        @Override
        public IArtifactKey getArtifactToBeWritten() {
            return newDescriptor.getArtifactKey();
        }

        @Override
        public boolean canBeginWrite() {
            return !committed;
        }

        @Override
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

        @Override
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

        @Override
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

        @Override
        public IArtifactDescriptor getArtifactFormatToBeWritten() {
            return newDescriptor;
        }

    }

    static IStatus errorStatus(String message, Throwable cause) {
        return errorStatus(message, cause, 0);
    }

    static IStatus errorStatus(String message, Throwable cause, int code) {
        return new Status(IStatus.ERROR, BUNDLE_ID, code, message, cause);
    }
}
