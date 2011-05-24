/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;

public class ModuleMetadataRepositoryFactory extends MetadataRepositoryFactory {
    private static final String REPOSITORY_TYPE = ModuleMetadataRepository.class.getSimpleName();

    @Override
    public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        throw RepositoryFactoryTools.unsupportedCreation(REPOSITORY_TYPE);
    }

    @Override
    public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
        File repositoryDir = RepositoryFactoryTools.asFile(location);
        if (repositoryDir != null) {
            return load(repositoryDir, flags);
        }
        return null;
    }

    private IMetadataRepository load(File repositoryDir, int flags) throws ProvisionException {
        if (ModuleMetadataRepository.canAttemptRead(repositoryDir)) {
            RepositoryFactoryTools.verifyModifiableNotRequested(flags, REPOSITORY_TYPE);
            return new ModuleMetadataRepository(getAgent(), repositoryDir);
        }
        return null;
    }
}
