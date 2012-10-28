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
package org.eclipse.tycho.repository.p2base.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderException;
import org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;

public class RepositoryArtifactProvider implements IRawArtifactProvider {

    private final IArtifactRepositoryManager repositoryManager;
    private final List<URI> repositoryURLs;
    final ArtifactTransferPolicy transferPolicy;

    List<IArtifactRepository> repositories;

    public RepositoryArtifactProvider(List<URI> artifactRepositories, ArtifactTransferPolicy transferPolicy,
            IProvisioningAgent agent) {
        this.repositoryURLs = artifactRepositories;
        this.transferPolicy = transferPolicy;
        this.repositoryManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

        if (this.repositoryManager == null) {
            throw new IllegalArgumentException("IArtifactRepositoryManager in p2 agent " + agent);
        }
    }

    private void init() throws ArtifactProviderException {
        if (repositories == null) {
            repositories = loadRepositories();
        }
    }

    private List<IArtifactRepository> loadRepositories() throws ArtifactProviderException {
        List<IArtifactRepository> result = new ArrayList<IArtifactRepository>(repositoryURLs.size());
        for (URI repositoryURL : repositoryURLs) {
            try {
                result.add(repositoryManager.loadRepository(repositoryURL, null));
            } catch (ProvisionException e) {
                throw new ArtifactProviderException(e);
            }
        }
        return result;
    }

    public boolean contains(IArtifactKey key) {
        init();
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(key))
                return true;
        }
        return false;
    }

    public boolean contains(IArtifactDescriptor descriptor) {
        init();
        for (IArtifactRepository repository : repositories) {
            if (repository.contains(descriptor))
                return true;
        }
        return false;
    }

    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        init();
        Set<IArtifactDescriptor> result = new HashSet<IArtifactDescriptor>();
        for (IArtifactRepository repository : repositories) {
            for (IArtifactDescriptor descriptor : repository.getArtifactDescriptors(key)) {
                result.add(descriptor);
            }
        }
        return result.toArray(new IArtifactDescriptor[result.size()]);
    }

    public IStatus getArtifact(IArtifactKey key, OutputStream destination, IProgressMonitor monitor) {
        init();

        for (IArtifactRepository repository : repositories) {
            IArtifactDescriptor[] availableFormats = repository.getArtifactDescriptors(key);
            if (availableFormats != null && availableFormats.length > 0) {

                IArtifactDescriptor preferredFormat = transferPolicy.pickFormat(availableFormats);
                IStatus status = repository.getArtifact(preferredFormat, destination, nonNull(monitor));

                return improveMessageIfError(status, repository, key.toString());
            }
        }
        return artifactNotFoundError(key.toString());
    }

    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
        init();

        for (IArtifactRepository repository : repositories) {
            if (repository.contains(descriptor)) {

                IStatus status = repository.getRawArtifact(descriptor, destination, nonNull(monitor));

                return improveMessageIfError(status, repository, descriptor.toString());
            }
        }
        String string = descriptor.toString();
        return artifactNotFoundError(string);
    }

    private IStatus improveMessageIfError(IStatus originalStatus, IArtifactRepository currentRepository,
            String currentArtifact) {

        if (originalStatus.matches(IStatus.ERROR | IStatus.CANCEL)) {
            String message = "An error occurred while transferring artifact " + currentArtifact + " from repository "
                    + currentRepository.getLocation();
            return new MultiStatus(Activator.ID, 0, new IStatus[] { originalStatus }, message, null);
        }
        return originalStatus;
    }

    private Status artifactNotFoundError(String artifact) {
        return new Status(IStatus.ERROR, Activator.ID, "Artifact " + artifact
                + " is not available in any of the following repositories: " + repositoryURLs);
    }

    // TODO share?
    private static IProgressMonitor nonNull(IProgressMonitor monitor) {
        if (monitor == null)
            return new NullProgressMonitor();
        else
            return monitor;
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor)
            throws ArtifactProviderException {
        init();
        return repositoriesAsQueriable().query(query, monitor);
    }

    private IQueryable<IArtifactKey> repositoriesAsQueriable() {
        int repositoryCount = repositories.size();
        if (repositoryCount == 1) {
            return repositories.get(0);
        } else {
            IArtifactRepository[] repositoriesArray = repositories.toArray(new IArtifactRepository[repositoryCount]);
            return new CompoundQueryable<IArtifactKey>(repositoriesArray);
        }
    }

}
