/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.p2;

import java.util.Collection;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;

public class P2RepositoryConnector implements RepositoryConnector {

    private final RemoteRepository repository;

    public P2RepositoryConnector(RemoteRepository repository) {
        this.repository = repository;
    }

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

    public void close() {
    }

}
