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
package org.eclipse.tycho.repository.local;

import java.io.File;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;

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

    public IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        ensureMirrored(key);
        return localArtifactRepository.getArtifact(key, destination, monitor);
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        ensureMirrored(descriptor.getArtifactKey());
        return localArtifactRepository.getRawArtifact(descriptor, destination, monitor);
    }

    public File getArtifactFile(IArtifactKey key) {
        ensureMirrored(key);
        return localArtifactRepository.getArtifactFile(key);
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        ensureMirrored(descriptor.getArtifactKey());
        return localArtifactRepository.getArtifactFile(descriptor);
    }

    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        ensureMirrored(key);
        return localArtifactRepository.getArtifactDescriptors(key);
    }

    public boolean contains(IArtifactDescriptor descriptor) {
        ensureMirrored(descriptor.getArtifactKey());
        return localArtifactRepository.contains(descriptor);
    }

    private void ensureMirrored(IArtifactKey key) {
        if (!localArtifactRepository.contains(key)) {
            try {
                // TODO handle non-existing correctly
                // TODO check status
                remoteProviders.getArtifact(key,
                        localArtifactRepository.getOutputStream(localArtifactRepository.createArtifactDescriptor(key)),
                        nonNull(null));
            } catch (ProvisionException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
        }
    }

    // TODO share?
    private static IProgressMonitor nonNull(IProgressMonitor monitor) {
        if (monitor == null)
            return new NullProgressMonitor();
        else
            return monitor;
    }
}
