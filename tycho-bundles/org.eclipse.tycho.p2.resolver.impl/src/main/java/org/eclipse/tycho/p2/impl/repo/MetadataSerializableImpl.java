/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
package org.eclipse.tycho.p2.impl.repo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.metadata.MetadataSerializable;

@SuppressWarnings("restriction")
public class MetadataSerializableImpl implements MetadataSerializable {
    private final IProvisioningAgent agent;

    public MetadataSerializableImpl() throws ProvisionException {
        super();
        this.agent = Activator.newProvisioningAgent();
    }

    @Override
    public void serialize(OutputStream stream, Set<?> installableUnits) throws IOException {
        final List<IInstallableUnit> units = toInstallableUnits(installableUnits);

        // TODO check if we can really "reuse" LocalMetadataRepository or should we implement our own Repository
        AbstractMetadataRepository targetRepo = new AbstractMetadataRepository(agent,
                "TychoTargetPlatform", LocalMetadataRepository.class.getName(), //$NON-NLS-1$
                "0.0.1", null, null, null, null) //$NON-NLS-1$
        {

            @Override
            public void initialize(RepositoryState state) {

            }

            @Override
            public Collection<IRepositoryReference> getReferences() {
                return Collections.emptyList();
            }

            @Override
            public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
                return query.perform(units.iterator());
            }

        };

        new MetadataRepositoryIO(agent).write(targetRepo, stream);
    }

    private List<IInstallableUnit> toInstallableUnits(Set<?> installableUnits) {
        ArrayList<IInstallableUnit> units = new ArrayList<>();

        for (Object o : installableUnits) {
            units.add((IInstallableUnit) o);
        }

        return units;
    }
}
