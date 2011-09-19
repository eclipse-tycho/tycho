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
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;

public class LocalArtifactRepositoryFactory extends ArtifactRepositoryFactory {

    private static final String REPOSITORY_TYPE = LocalArtifactRepository.class.getSimpleName();

    @Override
    public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        throw RepositoryFactoryTools.unsupportedCreation(REPOSITORY_TYPE);
    }

    @Override
    public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
        if ("file".equals(location.getScheme())) {
            final File localRepositoryDirectory = new File(location);
            if (localRepositoryDirectory.isDirectory()
                    && new File(localRepositoryDirectory, LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH).exists()) {
                return new LocalArtifactRepository(getAgent(), localRepositoryDirectory);
            }
        }
        return null;
    }
}
