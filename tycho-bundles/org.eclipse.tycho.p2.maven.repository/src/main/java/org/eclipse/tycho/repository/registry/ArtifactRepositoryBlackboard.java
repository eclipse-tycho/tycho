/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.registry;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.eclipse.tycho.repository.util.internal.RepositoryFactoryTools;

public class ArtifactRepositoryBlackboard extends ArtifactRepositoryFactory {

    private static HashMap<URI, IArtifactRepository> registry = new HashMap<URI, IArtifactRepository>();

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
