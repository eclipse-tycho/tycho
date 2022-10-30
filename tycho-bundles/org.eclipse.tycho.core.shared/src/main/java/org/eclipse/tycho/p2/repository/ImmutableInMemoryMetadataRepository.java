/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
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
package org.eclipse.tycho.p2.repository;

import static org.eclipse.tycho.repository.util.BundleConstants.BUNDLE_ID;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IPool;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

public class ImmutableInMemoryMetadataRepository implements IMetadataRepository {

    private final QueryableCollection units;

    public ImmutableInMemoryMetadataRepository(Set<IInstallableUnit> units) {
        this.units = new QueryableCollection(units);
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return units.query(query, monitor);
    }

    @Override
    public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
        try {
            runnable.run(monitor);
        } catch (OperationCanceledException e) {
            return new Status(IStatus.CANCEL, BUNDLE_ID, e.getMessage(), e);
        } catch (Exception e) {
            return new Status(IStatus.ERROR, BUNDLE_ID, e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }

    // defaulted getters

    @Override
    public URI getLocation() {
        return URI.create("memory:" + super.hashCode());
    }

    @Override
    public String getType() {
        return ImmutableInMemoryMetadataRepository.class.getName();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getName() {
        return super.toString();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getProvider() {
        return "";
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public IProvisioningAgent getProvisioningAgent() {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public Collection<IRepositoryReference> getReferences() {
        return Collections.emptyList();
    }

    // mutators (unsupported)

    @Override
    public boolean isModifiable() {
        return false;
    }

    @Override
    public String setProperty(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String setProperty(String key, String value, IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addReferences(Collection<? extends IRepositoryReference> references) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void compress(IPool<IInstallableUnit> iuPool) {
        throw new UnsupportedOperationException();
    }

}
