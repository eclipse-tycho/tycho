/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import static org.eclipse.tycho.repository.util.BundleConstants.BUNDLE_ID;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.util.StatusTool;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

public class MirroringArtifactProvider implements IRawArtifactFileProvider {

    private IRawArtifactProvider remoteProviders;
    private LocalArtifactRepository localArtifactRepository;

    public MirroringArtifactProvider(LocalArtifactRepository localArtifactRepository,
            IRawArtifactProvider remoteProviders) {
        this.remoteProviders = remoteProviders;
        this.localArtifactRepository = localArtifactRepository;
    }

    // pass through methods

    public boolean contains(IArtifactKey key) {
        if (localArtifactRepository.contains(key)) {
            return true;
        }
        return remoteProviders.contains(key);
    }

    @SuppressWarnings({ "restriction", "unchecked" })
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        IQueryable<IArtifactKey>[] sources = new IQueryable[] { localArtifactRepository, remoteProviders };
        return new CompoundQueryable<IArtifactKey>(sources).query(query, nonNull(monitor));
    }

    // mirroring methods

    public File getArtifactFile(IArtifactKey key) throws MirroringFailedException {
        if (ensureMirroredIfAvailable(key)) {
            return localArtifactRepository.getArtifactFile(key);
        }
        return null;
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) throws MirroringFailedException {
        if (ensureMirroredIfAvailable(descriptor.getArtifactKey())) {
            return localArtifactRepository.getArtifactFile(descriptor);
        }
        return null;
    }

    public IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException,
            MirroringFailedException {
        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        if (ensureMirroredIfAvailable(requestedKey)) {
            return localArtifactRepository.getArtifact(sink, monitor);
        }
        return artifactNotFoundStatus(requestedKey);
    }

    public IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException,
            MirroringFailedException {
        IArtifactKey requestedKey = sink.getArtifactToBeWritten();
        if (ensureMirroredIfAvailable(requestedKey)) {
            return localArtifactRepository.getRawArtifact(sink, monitor);
        }
        return artifactNotFoundStatus(requestedKey);
    }

    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) throws MirroringFailedException {
        if (ensureMirroredIfAvailable(key)) {
            return localArtifactRepository.getArtifactDescriptors(key);
        }
        return new IArtifactDescriptor[0];
    }

    public boolean contains(IArtifactDescriptor descriptor) throws MirroringFailedException {
        if (ensureMirroredIfAvailable(descriptor.getArtifactKey())) {
            return localArtifactRepository.contains(descriptor);
        }
        return false;
    }

    /**
     * @return <code>false</code> if the artifact is neither already cached locally nor available
     *         remotely.
     */
    private boolean ensureMirroredIfAvailable(IArtifactKey key) throws MirroringFailedException {
        // TODO mirror raw artifacts if enabled
        if (localArtifactRepository.contains(key)) {
            return true;
        } else if (remoteProviders.contains(key)) {
            performMirroring(key);
            return true;
        } else {
            return false;
        }
    }

    private void performMirroring(IArtifactKey key) throws MirroringFailedException {
        try {
            IArtifactSink localSink = localArtifactRepository.newAddingArtifactSink(key);
            IStatus transferStatus = remoteProviders.getArtifact(localSink, null);
            if (transferStatus.matches(IStatus.ERROR | IStatus.CANCEL)) {
                // TODO better formatted log?
                throw new MirroringFailedException("Could not mirror artifact " + key
                        + " into the local Maven repository: " + StatusTool.collectProblems(transferStatus),
                        StatusTool.findException(transferStatus));
            }
            // TODO log warnings in transferStatus
        } catch (ProvisionException e) {
            throw new MirroringFailedException("Error while mirroring artifact " + key
                    + " into the local Maven repository" + e.getMessage(), e);
        } catch (ArtifactSinkException e) {
            throw new MirroringFailedException("Error while mirroring artifact " + key
                    + " into the local Maven repository" + e.getMessage(), e);
        }
    }

    private static IStatus artifactNotFoundStatus(IArtifactKey key) {
        return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + key
                + " is neither available in the local Maven repository nor in the configured remote repositories", null);
    }

    // TODO share?
    private static IProgressMonitor nonNull(IProgressMonitor monitor) {
        if (monitor == null)
            return new NullProgressMonitor();
        else
            return monitor;
    }

    public static class MirroringFailedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MirroringFailedException(String message, Throwable cause) {
            super(message, cause);
        }

    }
}
