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
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.Activator;
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
     * descriptor instances stored in the index (i.e. the {@link #descriptors} member). A valid
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
     * This method is called for looking up entries from the index based on an
     * {@link IArtifactDescriptor} argument. This is for example the case in
     * {@link #contains(IArtifactDescriptor)} and {@link #removeDescriptor(IArtifactDescriptor)}.
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

        Set<ArtifactDescriptorT> descriptorsForKey = descriptorsMap.get(comparableDescriptor.getArtifactKey());
        if (descriptorsForKey != null) {
            descriptorsForKey.remove(comparableDescriptor);
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

    // TODO 393004 return null if contains(descriptor) == false
    public abstract File getArtifactFile(IArtifactDescriptor descriptor);

    public final File getArtifactFile(IArtifactKey key) {
        Set<ArtifactDescriptorT> descriptors = descriptorsMap.get(key);

        // if available, return location of canonical format of the artifact
        if (descriptors != null) {
            for (ArtifactDescriptorT descriptor : descriptors) {
                if (ArtifactTransferPolicy.isCanonicalFormat(descriptor)) {
                    return getArtifactFile(descriptor);
                }
            }
        }
        return null;
    }

    public final IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        IArtifactDescriptor[] availableFormats = getArtifactDescriptors(key);
        // TODO 393004 check for null/empty
        IArtifactDescriptor preferredFormat = transferPolicy.pickFormat(availableFormats);

        // TODO 393004 this is wrong; we must perform the mandatory processing steps here
        return getRawArtifact(preferredFormat, destination, monitor);
    }

    public final IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination,
            IProgressMonitor monitor) {
        try {
            InputStream source = new FileInputStream(getArtifactFile(descriptor));

            // copy to destination and close source
            FileUtils.copyStream(source, true, destination, false);

        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.ID, "I/O exception while reading artifact " + descriptor, e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public final OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
        ArtifactDescriptorT internalDescriptor = getInternalDescriptorForAdding(descriptor);
        File file = getArtifactFile(internalDescriptor);
        file.getParentFile().mkdirs();

        // TODO 393004 Only once the file is written completely, the descriptor may be added
        internalAddInternalDescriptor(internalDescriptor);
        internalStore(null);

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            // TODO revise message?
            throw new ProvisionException("Could not create artifact file", e);
        }
    }

}
