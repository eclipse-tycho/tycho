/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP SE and others.
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
package org.eclipse.tycho.p2.maven.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.eclipse.tycho.repository.util.RepositoryFactoryTools;

public class ArtifactRepositoryBlackboard extends ArtifactRepositoryFactory {

    private static HashMap<URI, IArtifactRepository> registry = new HashMap<>();

    public static synchronized void putRepository(RepositoryBlackboardKey key, IArtifactRepository repository) {
        registry.put(key.toURI(), repository);
    }

    @Override
    public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        throw RepositoryFactoryTools.unsupportedCreation(getClass());
    }

    @Override
    public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
        if (RepositoryBlackboardKey.SCHEME.equals(location.getScheme())) {
            return getRegisteredRepositoryOrNull(location, getAgent());
        }
        return null;
    }

    private static synchronized IArtifactRepository getRegisteredRepositoryOrNull(URI location, IProvisioningAgent agent)
            throws ProvisionException {
        return registry.get(location);
    }

}
