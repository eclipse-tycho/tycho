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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

class RemoteMetadataRepositoryManager implements IMetadataRepositoryManager {

    private final IMetadataRepositoryManager delegate;
    private final RemoteRepositoryHelper repositoryHelper;

    RemoteMetadataRepositoryManager(IMetadataRepositoryManager delegate, RemoteRepositoryHelper repositoryHelper) {
        this.delegate = delegate;
        this.repositoryHelper = repositoryHelper;
    }

    private URI translate(URI location) {
        return repositoryHelper.getEffectiveLocation(location);
    }

    private URI translateAndPrepareLoad(URI location) throws ProvisionException {
        return repositoryHelper.getEffectiveLocationAndPrepareLoad(location);
    }

    // delegate methods

    public void addRepository(URI location) {
        delegate.addRepository(translate(location));
    }

    public boolean contains(URI location) {
        return delegate.contains(translate(location));
    }

    public IMetadataRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException, OperationCanceledException {
        // not needed for remote repositories
        throw new UnsupportedOperationException();
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

    public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException, OperationCanceledException {
        return delegate.loadRepository(translateAndPrepareLoad(location), flags, monitor);
    }

    public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException,
            OperationCanceledException {
        return delegate.loadRepository(translateAndPrepareLoad(location), monitor);
    }

    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return delegate.query(query, monitor);
    }

    public IMetadataRepository refreshRepository(URI location, IProgressMonitor monitor) throws ProvisionException,
            OperationCanceledException {
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
