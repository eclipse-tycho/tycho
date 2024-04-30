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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.repository.LazyArtifactRepository;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2.repository.RepositoryArtifactProvider;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;

public class URITargetDefinitionContent implements TargetDefinitionContent {

    private IArtifactRepository artifactRepository;
    private IProvisioningAgent agent;
    private URI location;
    private String id;
    private IMetadataRepository metadataRepository;
    private ReferencedRepositoryMode referencedRepositoryMode;
    private MavenLogger logger;

    public URITargetDefinitionContent(IProvisioningAgent agent, URI location, String id,
            ReferencedRepositoryMode referencedRepositoryMode, MavenLogger logger) {
        this.agent = agent;
        this.location = location;
        this.id = id;
        this.referencedRepositoryMode = referencedRepositoryMode;
        this.logger = logger;
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

    private synchronized void preload(IProgressMonitor monitor) {
        if (metadataRepository == null) {
            Map<URI, IMetadataRepository> metadataRepositoriesMap = new LinkedHashMap<>();
            Map<URI, IArtifactRepository> artifactRepositoriesMap = new LinkedHashMap<>();
            URI uri = location;
            try {
                loadMetadataRepositories(uri, id, metadataRepositoriesMap, artifactRepositoriesMap,
                        referencedRepositoryMode == ReferencedRepositoryMode.include, agent, monitor, logger);
                loadArtifactRepositories(uri, artifactRepositoriesMap, agent, false, logger);
            } catch (ProvisionException e) {
                throw new TargetDefinitionResolutionException(
                        "Failed to load p2 metadata repository from location " + uri, e);
            }
            Collection<IMetadataRepository> metadataRepositories = metadataRepositoriesMap.values();
            if (metadataRepositories.size() == 1) {
                metadataRepository = metadataRepositories.iterator().next();
            } else {
                metadataRepository = new ListCompositeMetadataRepository(List.copyOf(metadataRepositories), agent);
            }
            Collection<IArtifactRepository> artifactRepositories = artifactRepositoriesMap.values();
            if (artifactRepositories.size() == 1) {
                artifactRepository = artifactRepositories.iterator().next();
            } else {
                artifactRepository = new ListCompositeArtifactRepository(List.copyOf(artifactRepositories), agent);
            }
        }
    }

    private static void loadMetadataRepositories(URI uri, String id, Map<URI, IMetadataRepository> metadataRepositories,
            Map<URI, IArtifactRepository> artifactRepositories, boolean includeReferenced, IProvisioningAgent agent,
            IProgressMonitor monitor, MavenLogger logger) throws ProvisionException {
        URI key = uri.normalize();
        if (metadataRepositories.containsKey(key)) {
            //already loaded...
            return;
        }
        SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
        IMetadataRepositoryManager metadataManager = agent.getService(IMetadataRepositoryManager.class);
        if (metadataManager == null) {
            throw new TargetDefinitionResolutionException("IMetadataRepositoryManager is null in IProvisioningAgent");
        }
        IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
        if (repositoryIdManager != null) {
            repositoryIdManager.addMapping(id, uri);
        }
        IMetadataRepository repository = metadataManager.loadRepository(uri, subMonitor.split(50));
        metadataRepositories.put(key, repository);
        if (includeReferenced) {
            Collection<IRepositoryReference> references = repository.getReferences();
            subMonitor.setWorkRemaining(references.size());
            for (IRepositoryReference reference : references) {
                if ((reference.getOptions() & IRepository.ENABLED) != 0) {
                    URI location = reference.getLocation();
                    if (reference.getType() == IRepository.TYPE_METADATA) {
                        try {
                            loadMetadataRepositories(location, reference.getNickname(), metadataRepositories,
                                    artifactRepositories, includeReferenced, agent, subMonitor.split(1), logger);
                        } catch (ProvisionException e) {
                            logger.warn("Skip referenced repository: " + location + ": " + e);
                        }
                    } else if (reference.getType() == IRepository.TYPE_ARTIFACT) {
                        loadArtifactRepositories(location, artifactRepositories, agent, true, logger);
                        subMonitor.worked(1);
                    }
                }
            }
        }

    }

    private static void loadArtifactRepositories(URI uri, Map<URI, IArtifactRepository> artifactRepositories,
            IProvisioningAgent agent, boolean referenced, MavenLogger logger) {
        URI key = uri.normalize();
        if (artifactRepositories.containsKey(key)) {
            //already loaded...
            return;
        }
        //artifact repositories are resolved lazy here as loading them might not be always necessary (e.g only dependency resolution required) and could be expensive (net I/O)
        LazyArtifactRepository repository;
        if (referenced) {
            repository = new LazyArtifactRepository(agent, uri, (u, a) -> {
                IArtifactRepositoryManager repositoryManager = agent.getService(IArtifactRepositoryManager.class);
                if (repositoryManager != null) {
                    try {
                        return repositoryManager.loadRepository(u, null);
                    } catch (ProvisionException e) {
                        logger.warn("Skip referenced repository: " + u + ": " + e);
                    }
                }
                return new EmptyArtifactRepository(a, u);
            });
        } else {
            repository = new LazyArtifactRepository(agent, uri, RepositoryArtifactProvider::loadRepository);
        }
        artifactRepositories.put(key, repository);
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        preload(null);
        return artifactRepository;
    }

}
