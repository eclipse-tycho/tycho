/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

/**
 * A {@link ICompositeRepository}/ {@link IMetadataRepository} that is backed by a simple list, in
 * contrast to the default P2 this does not require any access to the repository manager and simply
 * aggregates all data
 *
 */
public class ListCompositeMetadataRepository extends AbstractMetadataRepository
        implements ICompositeRepository<IInstallableUnit> {

    private List<? extends IMetadataRepository> metadataRepositories;

    public ListCompositeMetadataRepository(List<? extends IMetadataRepository> metadataRepositories,
            IProvisioningAgent agent) {
        super(agent);
        try {
            setLocation(new URI("list:" + UUID.randomUUID()));
        } catch (URISyntaxException e) {
            throw new AssertionError("should never happen", e);
        }
        setType(IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY);
        this.metadataRepositories = metadataRepositories;
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        int size = metadataRepositories.size();
        if (size == 1) {
            return metadataRepositories.get(0).query(query, monitor);
        }
        Collector<IInstallableUnit> collector = new Collector<>();
        SubMonitor subMonitor = SubMonitor.convert(monitor, size);
        for (IMetadataRepository repository : metadataRepositories) {
            collector.addAll(repository.query(query, subMonitor.split(1)));
        }
        return collector;
    }

    @Override
    public Collection<IRepositoryReference> getReferences() {
        List<IRepositoryReference> list = new ArrayList<>();
        for (IMetadataRepository repository : metadataRepositories) {
            list.addAll(repository.getReferences());
        }
        return list;
    }

    @Override
    public void addChild(URI child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<URI> getChildren() {
        List<URI> list = new ArrayList<>();
        for (IMetadataRepository repository : metadataRepositories) {
            list.add(repository.getLocation());
        }
        return list;
    }

    @Override
    public void removeAllChildren() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void removeChild(URI child) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void initialize(RepositoryState state) {
        // noop

    }

}
