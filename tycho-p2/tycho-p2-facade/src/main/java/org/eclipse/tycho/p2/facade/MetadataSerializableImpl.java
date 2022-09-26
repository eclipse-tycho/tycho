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
package org.eclipse.tycho.p2.facade;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

@Component(role = MetadataSerializable.class)
public class MetadataSerializableImpl implements MetadataSerializable {
    @Requirement
    private IProvisioningAgent agent;

    @Override
    public void serialize(OutputStream stream, Set<IInstallableUnit> installableUnits) throws IOException {
        // TODO check if we can really "reuse" LocalMetadataRepository or should we implement our own Repository
        AbstractMetadataRepository targetRepo = new AbstractMetadataRepository(agent, "TychoTargetPlatform", //$NON-NLS-1$
                LocalMetadataRepository.class.getName(), "0.0.1", null, null, null, null) {

            @Override
            public void initialize(RepositoryState state) {

            }

            @Override
            public Collection<IRepositoryReference> getReferences() {
                return Collections.emptyList();
            }

            @Override
            public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
                return query.perform(installableUnits.iterator());
            }

        };

        new MetadataRepositoryIO(agent).write(targetRepo, stream);
    }

}
