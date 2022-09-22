/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
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
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.core.shared.MavenLogger;

public class RemoteMetadataRepositoryManager implements IMetadataRepositoryManager {

    private IRepositoryIdManager loadingHelper;
    private IMetadataRepositoryManager service;
    private MavenLogger logger2;

    public RemoteMetadataRepositoryManager(IRepositoryIdManager loadingHelper, IMetadataRepositoryManager service,
            MavenLogger logger2) {
        this.loadingHelper = loadingHelper;
        this.service = service;
        this.logger2 = logger2;
    }

    private URI translate(URI location) {
        return loadingHelper.getEffectiveLocation(location);
    }

    private URI translateAndPrepareLoad(URI location) throws ProvisionException {
        return loadingHelper.getEffectiveLocationAndPrepareLoad(location);
    }

    @Override
    public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor)
            throws ProvisionException, OperationCanceledException {
        return this.loadRepository(location, IRepository.NONE, monitor);
    }

    @Override
    public IMetadataRepository loadRepository(URI location, int flags, IProgressMonitor monitor)
            throws ProvisionException, OperationCanceledException {
        URI effectiveLocation = translateAndPrepareLoad(location);

        IMetadataRepository loadedRepository = getDelegate().loadRepository(effectiveLocation, flags, monitor);
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
                logger2.error("Partial IU: " + unit.getId());
            }
        }
        if (hasPartialIUs) {
            String message = "The p2 repository at " + effectiveLocation
                    + " contains partial IUs (see above) from an old style update site which cannot be used for dependency resolution";
            throw new ProvisionException(message);
        }
    }

    // delegated methods

    @Override
    public void addRepository(URI location) {
        getDelegate().addRepository(translate(location));
    }

    @Override
    public boolean contains(URI location) {
        return getDelegate().contains(translate(location));
    }

    @Override
    public IMetadataRepository createRepository(URI location, String name, String type, Map<String, String> properties)
            throws ProvisionException, OperationCanceledException {
        return getDelegate().createRepository(translate(location), name, type, properties);
    }

    @Override
    public IProvisioningAgent getAgent() {
        return getDelegate().getAgent();
    }

    @Override
    public URI[] getKnownRepositories(int flags) {
        return getDelegate().getKnownRepositories(flags);
    }

    @Override
    public String getRepositoryProperty(URI location, String key) {
        return getDelegate().getRepositoryProperty(translate(location), key);
    }

    @Override
    public boolean isEnabled(URI location) {
        return getDelegate().isEnabled(translate(location));
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return getDelegate().query(query, monitor);
    }

    @Override
    public IMetadataRepository refreshRepository(URI location, IProgressMonitor monitor)
            throws ProvisionException, OperationCanceledException {
        return getDelegate().refreshRepository(translateAndPrepareLoad(location), monitor);
    }

    @Override
    public boolean removeRepository(URI location) {
        return getDelegate().removeRepository(translate(location));
    }

    @Override
    public void setEnabled(URI location, boolean enablement) {
        getDelegate().setEnabled(translate(location), enablement);
    }

    @Override
    public void setRepositoryProperty(URI location, String key, String value) {
        getDelegate().setRepositoryProperty(translate(location), key, value);
    }

    private IMetadataRepositoryManager getDelegate() {
        return service;
    }

}
