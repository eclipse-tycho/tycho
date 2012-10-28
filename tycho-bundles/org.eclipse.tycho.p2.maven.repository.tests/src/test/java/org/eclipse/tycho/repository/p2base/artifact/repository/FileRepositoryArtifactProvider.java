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
 * {@link RepositoryArtifactProvider} implementation which adds file capabilities. Currently only
 * needed in tests.
 */
public class FileRepositoryArtifactProvider extends RepositoryArtifactProvider implements IRawArtifactFileProvider {

    public FileRepositoryArtifactProvider(List<URI> artifactRepositories, ArtifactTransferPolicy transferPolicy,
            IProvisioningAgent agent) {
        super(artifactRepositories, transferPolicy, agent);
    }

    public File getArtifactFile(IArtifactKey key) {
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(key)) {
                return ((IFileArtifactRepository) repository).getArtifactFile(key);
            }
        }
        return null;
    }

    public File getArtifactFile(IArtifactDescriptor descriptor) {
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(descriptor)) {
                return ((IFileArtifactRepository) repository).getArtifactFile(descriptor);
            }
        }
        return null;
    }

}
