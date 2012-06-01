/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;

@SuppressWarnings("restriction")
class P2MirrorDisablingArtifactRepositoryManager implements IArtifactRepositoryManager {

    private IArtifactRepositoryManager delegate;

    public P2MirrorDisablingArtifactRepositoryManager(IArtifactRepositoryManager originalRepositoryManager) {
        this.delegate = originalRepositoryManager;
    }

    private static IArtifactRepository disableMirrors(IArtifactRepository repository) throws ProvisionException {
        if (repository instanceof SimpleArtifactRepository) {
            stripMirrorsURLProperty((SimpleArtifactRepository) repository);
        }
        return repository;
    }

    private static void stripMirrorsURLProperty(AbstractRepository<?> repository) {
        try {
            Map<?, ?> properties = getRepositoryProperties(repository);
            properties.remove(IRepository.PROP_MIRRORS_URL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable mirrors for artifact repository at \""
                    + repository.getLocation() + "\"", e);
        }
    }

    private static Map<?, ?> getRepositoryProperties(AbstractRepository<?> repository) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        // TODO there should be a better way to modify repository properties
        Field field = AbstractRepository.class.getDeclaredField("properties");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(repository);
    }

    // disable mirrors in returned repositories

    public IArtifactRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        IArtifactRepository repository = delegate.createRepository(location, name, type, properties);
        disableMirrors(repository);
        return repository;
    }

    public IArtifactRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException {
        IArtifactRepository repository = delegate.loadRepository(location, flags, monitor);
        disableMirrors(repository);
        return repository;
    }

    public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        IArtifactRepository repository = delegate.loadRepository(location, monitor);
        disableMirrors(repository);
        return repository;
    }

    public IArtifactRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        IArtifactRepository repository = delegate.refreshRepository(location, monitor);
        disableMirrors(repository);
        return repository;
    }

    // plain delegation

    public void addRepository(URI location) {
        delegate.addRepository(location);
    }

    public boolean contains(URI location) {
        return delegate.contains(location);
    }

    public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
            Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties) {
        return delegate.createMirrorRequest(key, destination, destinationDescriptorProperties,
                destinationRepositoryProperties);
    }

    public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
            Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties,
            String downloadStatsParameters) {
        return delegate.createMirrorRequest(key, destination, destinationDescriptorProperties,
                destinationRepositoryProperties, downloadStatsParameters);
    }

    public IProvisioningAgent getAgent() {
        return delegate.getAgent();
    }

    public URI[] getKnownRepositories(int flags) {
        return delegate.getKnownRepositories(flags);
    }

    public String getRepositoryProperty(URI location, String key) {
        return delegate.getRepositoryProperty(location, key);
    }

    public boolean isEnabled(URI location) {
        return delegate.isEnabled(location);
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return delegate.query(query, monitor);
    }

    public boolean removeRepository(URI location) {
        return delegate.removeRepository(location);
    }

    public void setEnabled(URI location, boolean enablement) {
        delegate.setEnabled(location, enablement);
    }

    public void setRepositoryProperty(URI location, String key, String value) {
        delegate.setRepositoryProperty(location, key, value);
    }

}
