/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.util.Collection;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;

public class TargetRepositoryConnector implements RepositoryConnector {

    private final RemoteRepository repository;

    public TargetRepositoryConnector(RemoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void get(Collection<? extends ArtifactDownload> artifactDownloads,
            Collection<? extends MetadataDownload> metadataDownloads) {
        if (artifactDownloads != null) {
            for (ArtifactDownload a : artifactDownloads) {
                a.setException(new ArtifactNotFoundException(a.getArtifact(), repository));
            }
        }
        if (metadataDownloads != null) {
            for (MetadataDownload m : metadataDownloads) {
                m.setException(new MetadataNotFoundException(m.getMetadata(), repository));
            }
        }
    }

    @Override
    public void put(Collection<? extends ArtifactUpload> artifactUploads,
            Collection<? extends MetadataUpload> metadataUploads) {
        if (artifactUploads != null) {
            for (ArtifactUpload a : artifactUploads) {
                a.setException(new ArtifactNotFoundException(a.getArtifact(), repository));
            }
        }
        if (metadataUploads != null) {
            for (MetadataUpload m : metadataUploads) {
                m.setException(new MetadataNotFoundException(m.getMetadata(), repository));
            }
        }
    }

    @Override
    public void close() {
    }

}
