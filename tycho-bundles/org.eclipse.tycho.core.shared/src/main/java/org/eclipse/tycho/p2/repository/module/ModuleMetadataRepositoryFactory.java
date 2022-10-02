/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
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
package org.eclipse.tycho.p2.repository.module;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.tycho.repository.util.RepositoryFactoryTools;

public class ModuleMetadataRepositoryFactory extends MetadataRepositoryFactory {

    @Override
    public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        File repositoryDir = RepositoryFactoryTools.asFile(location);
        if (repositoryDir == null) {
            throw RepositoryFactoryTools.invalidCreationLocation(ModuleMetadataRepository.REPOSITORY_TYPE, location);
        }
        return new ModuleMetadataRepository(getAgent(), repositoryDir);
        // ignore name and properties because repository type cannot persist it
    }

    @Override
    public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
        File repositoryDir = RepositoryFactoryTools.asFile(location);
        if (repositoryDir != null) {
            return load(repositoryDir);
        }
        return null;
    }

    private IMetadataRepository load(File repositoryDir) throws ProvisionException {
        if (ModuleMetadataRepository.canAttemptRead(repositoryDir)) {
            return new ModuleMetadataRepository(getAgent(), repositoryDir);
        }
        return null;
    }
}
