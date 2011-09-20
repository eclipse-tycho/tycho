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
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.repository.util.RepositoryFactoryTools;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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
                    && new File(localRepositoryDirectory, ".meta/p2-artifacts.properties").exists()) {
                // see FileBasedTychoRepositoryIndex#ARTIFACTS_INDEX_RELPATH
                return new LocalArtifactRepository(getAgent(), lookupLocalRepoIndices());
            }
        }
        return null;
    }

    protected LocalRepositoryP2Indices lookupLocalRepoIndices() {
        final BundleContext context = Activator.getContext();
        ServiceReference<LocalRepositoryP2Indices> localP2RepoReference = context.getServiceReference(LocalRepositoryP2Indices.class);
        if (localP2RepoReference != null) {
            LocalRepositoryP2Indices localRepoIndices = context.getService(localP2RepoReference);
            if (localRepoIndices != null) {
                return localRepoIndices;
            }
        }
        throw new IllegalStateException("service not registered: " + LocalRepositoryP2Indices.class.getName());
    }
}
