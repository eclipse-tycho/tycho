/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *    Christoph Läubrich - Bug 538144 - Support other target locations (Directory, Features, Installations)
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;

/**
 * {@link RepositoryArtifactProvider} implementation which adds file access capabilities.
 */
public class FileRepositoryArtifactProvider extends RepositoryArtifactProvider implements IRawArtifactFileProvider {

    public FileRepositoryArtifactProvider(List<? extends IFileArtifactRepository> repositories,
            ArtifactTransferPolicy transferPolicy) {
        super(repositories, transferPolicy);
    }

    public FileRepositoryArtifactProvider(List<URI> artifactRepositories, ArtifactTransferPolicy transferPolicy,
            IProvisioningAgent agent) {
        super(artifactRepositories, transferPolicy, agent);
    }

    FileRepositoryArtifactProvider(ArtifactRepositorySupplier repositoryLoader, ArtifactTransferPolicy transferPolicy) {
        super(repositoryLoader, transferPolicy);
    }

    @Override
    protected void repositoriesLoaded() {
        for (IArtifactRepository repository : repositories) {
            if (!(repository instanceof IFileArtifactRepository)) {
                throw new IllegalArgumentException("Repository loaded from \"" + repository.getLocation()
                        + "\" is not a file system based artifact repository.");
            }
        }
    }

    @Override
    public File getArtifactFile(IArtifactKey key) {
        init();
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(key)) {
                return ((IFileArtifactRepository) repository).getArtifactFile(key);
            }
        }
        return null;
    }

    @Override
    public File getArtifactFile(IArtifactDescriptor descriptor) {
        init();
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(descriptor)) {
                return ((IFileArtifactRepository) repository).getArtifactFile(descriptor);
            }
        }
        return null;
    }

    @Override
    public boolean isFileAlreadyAvailable(IArtifactKey artifactKey) {
        return contains(artifactKey);
    }

}
