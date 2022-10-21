/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.agent;

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
import org.eclipse.tycho.IRepositoryIdManager;

class RemoteArtifactRepositoryManager implements IArtifactRepositoryManager {

    private IArtifactRepositoryManager delegate;
    private final IRepositoryIdManager loadingHelper;

    RemoteArtifactRepositoryManager(IArtifactRepositoryManager delegate, IRepositoryIdManager loadingHelper) {
        this.delegate = delegate;
        this.loadingHelper = loadingHelper;
    }

    private URI translate(URI location) {
        return loadingHelper.getEffectiveLocation(location);
    }

    private URI translateAndPrepareLoad(URI location) throws ProvisionException {
        return loadingHelper.getEffectiveLocationAndPrepareLoad(location);
    }

    @Override
    public void addRepository(URI location) {
        delegate.addRepository(translate(location));
    }

    @Override
    public boolean contains(URI location) {
        return delegate.contains(translate(location));
    }

    @Override
    public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
            Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties) {
        return delegate.createMirrorRequest(key, destination, destinationDescriptorProperties,
                destinationRepositoryProperties);
    }

    @Override
    public IArtifactRequest createMirrorRequest(IArtifactKey key, IArtifactRepository destination,
            Map<String, String> destinationDescriptorProperties, Map<String, String> destinationRepositoryProperties,
            String downloadStatsParameters) {
        return delegate.createMirrorRequest(key, destination, destinationDescriptorProperties,
                destinationRepositoryProperties, downloadStatsParameters);
    }

    @Override
    public IArtifactRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException {
        return delegate.createRepository(translate(location), name, type, properties);
    }

    @Override
    public IProvisioningAgent getAgent() {
        return delegate.getAgent();
    }

    @Override
    public URI[] getKnownRepositories(int flags) {
        return delegate.getKnownRepositories(flags);
    }

    @Override
    public String getRepositoryProperty(URI location, String key) {
        return delegate.getRepositoryProperty(translate(location), key);
    }

    @Override
    public boolean isEnabled(URI location) {
        return delegate.isEnabled(translate(location));
    }

    @Override
    public IArtifactRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException {
        return delegate.loadRepository(translateAndPrepareLoad(location), flags, monitor);
    }

    @Override
    public IArtifactRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        return delegate.loadRepository(translateAndPrepareLoad(location), monitor);
    }

    @Override
    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
        return delegate.query(query, monitor);
    }

    @Override
    public IArtifactRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
        return delegate.refreshRepository(translateAndPrepareLoad(location), monitor);
    }

    @Override
    public boolean removeRepository(URI location) {
        return delegate.removeRepository(translate(location));
    }

    @Override
    public void setEnabled(URI location, boolean enablement) {
        delegate.setEnabled(translate(location), enablement);
    }

    @Override
    public void setRepositoryProperty(URI location, String key, String value) {
        delegate.setRepositoryProperty(translate(location), key, value);
    }

}
