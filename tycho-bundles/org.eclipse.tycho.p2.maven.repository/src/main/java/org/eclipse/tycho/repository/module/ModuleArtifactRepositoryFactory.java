/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.tycho.repository.util.RepositoryFactoryTools;

public class ModuleArtifactRepositoryFactory extends ArtifactRepositoryFactory {

    @Override
    public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        File repositoryDir = RepositoryFactoryTools.asFile(location);
        if (repositoryDir == null) {
            throw RepositoryFactoryTools.invalidCreationLocation(ModuleArtifactRepository.REPOSITORY_TYPE, location);
        }
        return ModuleArtifactRepository.createInstance(getAgent(), repositoryDir);
        // ignore name and properties because repository type cannot persist it
    }

    @Override
    public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
        File repositoryDir = RepositoryFactoryTools.asFile(location);
        if (repositoryDir != null) {
            return load(repositoryDir, flags);
        }
        return null;
    }

    private IArtifactRepository load(File repositoryDir, int flags) throws ProvisionException {
        if (ModuleArtifactRepository.canAttemptRead(repositoryDir)) {
            return ModuleArtifactRepository.restoreInstance(getAgent(), repositoryDir);
        }
        return null;
    }
}
