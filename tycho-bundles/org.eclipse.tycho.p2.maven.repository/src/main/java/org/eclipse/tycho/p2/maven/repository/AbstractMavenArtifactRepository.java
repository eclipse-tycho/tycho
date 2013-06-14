/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - make readable as p2 artifact repository; implement IFileArtifactRepository
 *******************************************************************************/

package org.eclipse.tycho.p2.maven.repository;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;

/**
 * Base class for p2 artifact repositories with GAV-based artifact storage.
 */
// TODO 393004 obsolete; delete class
public abstract class AbstractMavenArtifactRepository extends AbstractArtifactRepository implements
        IFileArtifactRepository {
    public static final String VERSION = "1.0.0";

    private static final IArtifactDescriptor[] ARTIFACT_DESCRIPTOR_ARRAY = new IArtifactDescriptor[0];

    // TODO where do we need multiple descriptors per artifact key? do we support storing pack200 compressed files in the local Maven repo?
    protected Map<IArtifactKey, Set<IArtifactDescriptor>> descriptorsMap = new HashMap<IArtifactKey, Set<IArtifactDescriptor>>();

    protected Set<IArtifactDescriptor> descriptors = new HashSet<IArtifactDescriptor>();

    protected final RepositoryReader contentLocator;

    protected AbstractMavenArtifactRepository(IProvisioningAgent agent, URI uri, RepositoryReader contentLocator) {
        super(agent, "Maven Local Repository", AbstractMavenArtifactRepository.class.getName(), VERSION, uri, null,
                null, null);
        this.contentLocator = contentLocator;
    }

    @Override
    public boolean contains(IArtifactDescriptor descriptor) {
        if (descriptor == null)
            throw new NullPointerException();
        return descriptors.contains(descriptor);
    }

    @Override
    public boolean contains(IArtifactKey key) {
        if (key == null)
            throw new NullPointerException();
        return descriptorsMap.containsKey(key);
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Set<IArtifactDescriptor> descriptors = descriptorsMap.get(key);
        if (descriptors == null) {
            return ARTIFACT_DESCRIPTOR_ARRAY;
        }
        return descriptors.toArray(ARTIFACT_DESCRIPTOR_ARRAY);
    }

    protected GAV getP2GAV(IArtifactDescriptor descriptor) {
        IArtifactKey key = descriptor.getArtifactKey();
        StringBuffer version = new StringBuffer();
        key.getVersion().toString(version);
        return RepositoryLayoutHelper.getP2Gav(key.getClassifier(), key.getId(), version.toString());
    }

    public IStatus resolve(IArtifactDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
        super.addDescriptor(descriptor, monitor);
        internalAddDescriptor(descriptor);
        descriptorsChanged();
    }

    /**
     * Adds a descriptor without triggering {@link #descriptorsChanged()}.
     */
    protected final void internalAddDescriptor(IArtifactDescriptor descriptor) {
        descriptors.add(descriptor);

        IArtifactKey key = descriptor.getArtifactKey();
        Set<IArtifactDescriptor> descriptorsForKey = descriptorsMap.get(key);

        if (descriptorsForKey == null) {
            descriptorsForKey = new HashSet<IArtifactDescriptor>();
            descriptorsMap.put(key, descriptorsForKey);
        }

        descriptorsForKey.add(descriptor);
    }

    protected final void descriptorsChanged() {
        // TODO check if store is disabled
        store();
    }

    /**
     * Called whenever the list of descriptors needs to be persisted.
     */
    protected void store() {
        // TODO split this class in writable and non-writable repos to avoid empty implementation
    }

    @Override
    public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
        super.addDescriptors(descriptors, monitor);

        for (IArtifactDescriptor descriptor : descriptors) {
            internalAddDescriptor(descriptor);
        }
        descriptorsChanged();
    }

    GAV getGAV(IArtifactDescriptor descriptor) {
        GAV gav = RepositoryLayoutHelper.getGAV(((ArtifactDescriptor) descriptor).getProperties());

        if (gav == null) {
            gav = getP2GAV(descriptor);
        }

        return gav;
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return query.perform(descriptorsMap.keySet().iterator());
    }

    public IQueryable<IArtifactDescriptor> descriptorQueryable() {
        return new IQueryable<IArtifactDescriptor>() {
            public IQueryResult<IArtifactDescriptor> query(IQuery<IArtifactDescriptor> query, IProgressMonitor monitor) {
                return query.perform(descriptors.iterator());
            }
        };
    }

    // TODO shouldn't this be implemented in the super class from p2?
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

    @Override
    public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        // default: the artifacts are stored raw and don't need further processing
        return getRawArtifact(descriptor, destination, monitor);
    }

    @SuppressWarnings("restriction")
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        // TODO consolidate this logic with getArtifactFile
        GAV gav = getGAV(descriptor);
        String classifier = RepositoryLayoutHelper.getClassifier(descriptor.getProperties());
        String extension = RepositoryLayoutHelper.getExtension(descriptor.getProperties());

        if (IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT))) {
            classifier = RepositoryLayoutHelper.PACK200_CLASSIFIER;
            extension = RepositoryLayoutHelper.PACK200_EXTENSION;
        }

        try {
            InputStream source = new FileInputStream(
                    contentLocator.getLocalArtifactLocation(gav, classifier, extension));

            // copy to destination and close source 
            FileUtils.copyStream(source, true, destination, false);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, BUNDLE_ID, "I/O exception while reading artifact " + gav.toExternalForm()
                    + ":" + classifier + ":" + extension, e);
        }
        return Status.OK_STATUS;
    }

    public File getArtifactFile(IArtifactKey key) {
        Set<IArtifactDescriptor> descriptors = descriptorsMap.get(key);
        if (descriptors != null) {
            for (IArtifactDescriptor descriptor : descriptors) {
                if (descriptor.getProperty(IArtifactDescriptor.FORMAT) == null) {
                    return getArtifactFile(descriptor);
                }
            }
        }
        return null;
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        // TODO consolidate this logic with getRawArtifact
        GAV gav = getGAV(descriptor);
        String classifier = RepositoryLayoutHelper.getClassifier(descriptor.getProperties());
        String extension = RepositoryLayoutHelper.getExtension(descriptor.getProperties());

        // TODO where does this magic come from? the logic behind this should be made explicit and e.g. moved into a separate class
        // TODO bring together with other pack200 magic in org.eclipse.tycho.p2.maven.repository.MavenArtifactRepository.downloadArtifact(IArtifactDescriptor, OutputStream)
        if (IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT))) {
            classifier = RepositoryLayoutHelper.PACK200_CLASSIFIER;
            extension = RepositoryLayoutHelper.PACK200_EXTENSION;
        }

        return contentLocator.getLocalArtifactLocation(gav, classifier, extension);
    }
}
