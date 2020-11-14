/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.repository;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

public final class SupplierMetadataRepository extends AbstractMetadataRepository {

    private Supplier<Iterator<IInstallableUnit>> unitprovider;

    public SupplierMetadataRepository(IProvisioningAgent agent, Supplier<Iterator<IInstallableUnit>> unitprovider) {
        super(agent);
        this.unitprovider = unitprovider;
    }

    @Override
    public Collection<IRepositoryReference> getReferences() {
        return Collections.emptyList();
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return query.perform(unitprovider.get());
    }

    @Override
    public void initialize(RepositoryState state) {

    }

    @Override
    public synchronized void setLocation(URI location) {
        super.setLocation(location);
    }

}
