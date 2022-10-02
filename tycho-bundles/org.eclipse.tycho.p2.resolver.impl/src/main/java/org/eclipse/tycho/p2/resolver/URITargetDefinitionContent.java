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
package org.eclipse.tycho.p2.resolver;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.p2.repository.LazyArtifactRepository;
import org.eclipse.tycho.p2.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.p2.target.TargetDefinitionContent;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;

public class URITargetDefinitionContent implements TargetDefinitionContent {

    private final IArtifactRepository artifactRepository;
    private IProvisioningAgent agent;
    private URI location;
    private String id;
    private IMetadataRepository metadataRepository;

    public URITargetDefinitionContent(IProvisioningAgent agent, URI location, String id) {
        this.agent = agent;
        this.location = location;
        this.id = id;
        //artifact repositories are resolved lazy here as loading them might not be always necessary (e.g only dependency resolution required) and could be expensive (net I/O)
        artifactRepository = new LazyArtifactRepository(agent, location, RepositoryArtifactProvider::loadRepository);

    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 200);
        preload(subMonitor.split(100));
        subMonitor.setWorkRemaining(100);
        return getMetadataRepository().query(query, subMonitor.split(100));
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        preload(null);
        return metadataRepository;
    }

    public synchronized void preload(IProgressMonitor monitor) {
        if (metadataRepository == null) {
            IMetadataRepositoryManager metadataManager = agent.getService(IMetadataRepositoryManager.class);
            if (metadataManager == null) {
                throw new TargetDefinitionResolutionException(
                        "IMetadataRepositoryManager is null in IProvisioningAgent");
            }
            IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
            if (repositoryIdManager != null) {
                repositoryIdManager.addMapping(id, location);
            }
            try {
                metadataRepository = metadataManager.loadRepository(location, monitor);
            } catch (ProvisionException e) {
                throw new TargetDefinitionResolutionException(
                        "Failed to load p2 metadata repository from location " + location, e);
            }
        }
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

}
