/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
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
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.MavenLogger;

class RemoteMetadataRepositoryManager implements IMetadataRepositoryManager {

    private final IMetadataRepositoryManager delegate;
    private final RemoteRepositoryLoadingHelper loadingHelper;
    private final MavenLogger logger;

    RemoteMetadataRepositoryManager(IMetadataRepositoryManager delegate, RemoteRepositoryLoadingHelper loadingHelper,
            MavenLogger logger) {
        this.delegate = delegate;
        this.loadingHelper = loadingHelper;
        this.logger = logger;
    }

    private URI translate(URI location) {
        return loadingHelper.getEffectiveLocation(location);
    }

    private URI translateAndPrepareLoad(URI location) throws ProvisionException {
        return loadingHelper.getEffectiveLocationAndPrepareLoad(location);
    }

    public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException,
            OperationCanceledException {
        return this.loadRepository(location, IRepository.NONE, monitor);
    }

    public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException, OperationCanceledException {
        URI effectiveLocation = translateAndPrepareLoad(location);

        IMetadataRepository loadedRepository = delegate.loadRepository(effectiveLocation, flags, monitor);
        failIfRepositoryContainsPartialIUs(loadedRepository, effectiveLocation);

        return loadedRepository;
    }

    private void failIfRepositoryContainsPartialIUs(IMetadataRepository repository, URI effectiveLocation)
            throws ProvisionException {
        IQueryResult<IInstallableUnit> allUnits = repository.query(QueryUtil.ALL_UNITS, null);
        boolean hasPartialIUs = false;
        for (IInstallableUnit unit : allUnits.toUnmodifiableSet()) {
            if (Boolean.valueOf(unit.getProperty(IInstallableUnit.PROP_PARTIAL_IU))) {
                hasPartialIUs = true;
                logger.error("Partial IU: " + unit.getId());
            }
        }
        if (hasPartialIUs) {
            String message = "The p2 repository at "
                    + effectiveLocation
                    + " contains partial IUs (see above) from an old style update site which cannot be used for dependency resolution";
            throw new ProvisionException(message);
        }
    }

    // delegated methods

    public void addRepository(URI location) {
        delegate.addRepository(translate(location));
    }

    public boolean contains(URI location) {
        return delegate.contains(translate(location));
    }

    public IMetadataRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException, OperationCanceledException {
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
