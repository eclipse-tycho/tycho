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

import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

class RemoteArtifactRepositoryManager implements IArtifactRepositoryManager {

    private IArtifactRepositoryManager delegate;
    private final RemoteRepositoryHelper repositoryHelper;

    RemoteArtifactRepositoryManager(IArtifactRepositoryManager delegate, RemoteRepositoryHelper repositoryHelper) {
        this.delegate = delegate;
        this.repositoryHelper = repositoryHelper;
    }

    private URI translate(URI location) {
        return repositoryHelper.getEffectiveLocation(location);
    }

    private URI translateAndPrepareLoad(URI location) throws ProvisionException {
        return repositoryHelper.getEffectiveLocationAndPrepareLoad(location);
    }

    public void addRepository(URI location) {
        delegate.addRepository(translate(location));
    }

    public boolean contains(URI location) {
        return delegate.contains(translate(location));
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

    public IArtifactRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        return delegate.createRepository(translate(location), name, type, properties);
    }

    public IProvisioningAgent getAgent() {
        return delegate.getAgent();
    }

    public URI[] getKnownRepositories(int flags) {
        return delegate.getKnownRepositories(flags);
    }

    public String getRepositoryProperty(URI location, String key) {
        return delegate.getRepositoryProperty(translate(location), key);
    }

    public boolean isEnabled(URI location) {
        return delegate.isEnabled(translate(location));
    }

    public IArtifactRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException {
        return delegate.loadRepository(translateAndPrepareLoad(location), flags, monitor);
    }

    public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        return delegate.loadRepository(translateAndPrepareLoad(location), monitor);
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return delegate.query(query, monitor);
    }

    public IArtifactRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        return delegate.refreshRepository(translateAndPrepareLoad(location), monitor);
    }

    public boolean removeRepository(URI location) {
        return delegate.removeRepository(translate(location));
    }

    public void setEnabled(URI location, boolean enablement) {
        delegate.setEnabled(translate(location), enablement);
    }

    public void setRepositoryProperty(URI location, String key, String value) {
        delegate.setRepositoryProperty(translate(location), key, value);
    }

}
