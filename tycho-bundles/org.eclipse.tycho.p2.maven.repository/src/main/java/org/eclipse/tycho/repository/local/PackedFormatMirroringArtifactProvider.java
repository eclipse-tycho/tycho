/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.local;

import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

@Deprecated(forRemoval = true)
final class PackedFormatMirroringArtifactProvider extends MirroringArtifactProvider {

    PackedFormatMirroringArtifactProvider(LocalArtifactRepository localArtifactRepository,
            IRawArtifactProvider remoteProviders, MavenLogger logger) {
        super(localArtifactRepository, remoteProviders, logger);
    }

    @Override
    protected boolean makeOneFormatLocallyAvailable(IArtifactKey key)
            throws MirroringFailedException, ProvisionException, ArtifactSinkException {

        if (isAvailableLocally(key)) {
            return true;

        } else if (findPackedDescriptor(remoteProviders.getArtifactDescriptors(key)) != null) {
            // packed format is available remotely but not yet locally -> download it
            Lock downloadLock = localArtifactRepository.getLockForDownload(key);
            downloadLock.lock();
            try {
                if (!isAvailableLocally(key)) { // check again within lock
                    downloadArtifact(key);
                }
            } finally {
                downloadLock.unlock();
            }
            return true;

        } else {
            // no packed format available -> try to make at least the canonical format available
            return super.makeOneFormatLocallyAvailable(key);
        }
    }

    private boolean isAvailableLocally(IArtifactKey key) {
        return findPackedDescriptor(localArtifactRepository.getArtifactDescriptors(key)) != null;
    }

    @Override
    protected IStatus downloadMostSpecificNeededFormatOfArtifact(IArtifactKey key)
            throws ProvisionException, ArtifactSinkException {

        IArtifactDescriptor[] allDescriptors = remoteProviders.getArtifactDescriptors(key);
        IArtifactDescriptor packedDescriptor = findPackedDescriptor(allDescriptors);

        if (packedDescriptor != null) {
            // download only the raw format -> the canonical format can be created locally from that format
            // TODO 393004 remove "maven-groupId", etc. properties to force storage as p2/osgi/bundle...
            return downloadRawArtifact(packedDescriptor);

        } else {
            logger.debug("No remote repository provides " + key.getId() + "_" + key.getVersion()
                    + " in packed format. Only the canonical format will be available in the build.");
            return downloadCanonicalArtifact(key);
        }
    }

    private final IStatus downloadRawArtifact(IArtifactDescriptor descriptor)
            throws ProvisionException, ArtifactSinkException {
        // TODO 397355 ignore ProvisionException.ARTIFACT_EXISTS
        IRawArtifactSink localSink = localArtifactRepository.newAddingRawArtifactSink(descriptor);
        return remoteProviders.getRawArtifact(localSink, monitorForDownload());
    }

}
