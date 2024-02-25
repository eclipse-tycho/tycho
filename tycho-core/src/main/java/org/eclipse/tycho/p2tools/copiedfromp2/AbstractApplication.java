/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.p2.repository.ListCompositeMetadataRepository;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;

public abstract class AbstractApplication {
    protected boolean removeAddedRepositories = true;

    protected List<RepositoryDescriptor> sourceRepositories = new ArrayList<>(); // List of repository descriptors
    protected List<URI> artifactReposToRemove = new ArrayList<>();
    protected List<URI> metadataReposToRemove = new ArrayList<>();
    protected List<IInstallableUnit> sourceIUs = new ArrayList<>();
    private List<RepositoryDescriptor> destinationRepos = new ArrayList<>();

    protected IArtifactRepository destinationArtifactRepository = null;
    protected IMetadataRepository destinationMetadataRepository = null;

    private IMetadataRepository compositeMetadataRepository = null;
    private IArtifactRepository compositeArtifactRepository = null;

    protected IProvisioningAgent agent;

    public AbstractApplication(IProvisioningAgent agent) {
        this.agent = agent;
    }

    public void setSourceIUs(List<IInstallableUnit> ius) {
        sourceIUs = ius;
    }

    protected void finalizeRepositories() {
        if (removeAddedRepositories) {
            IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
            for (URI uri : artifactReposToRemove)
                artifactRepositoryManager.removeRepository(uri);
            IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
            for (URI uri : metadataReposToRemove)
                metadataRepositoryManager.removeRepository(uri);
        }
        metadataReposToRemove = null;
        artifactReposToRemove = null;
        compositeArtifactRepository = null;
        compositeMetadataRepository = null;
        destinationArtifactRepository = null;
        destinationMetadataRepository = null;
    }

    protected IMetadataRepositoryManager getMetadataRepositoryManager() {
        return agent.getService(IMetadataRepositoryManager.class);
    }

    protected IArtifactRepositoryManager getArtifactRepositoryManager() {
        return agent.getService(IArtifactRepositoryManager.class);
    }

    public void initializeRepos(IProgressMonitor progress) throws ProvisionException {
        IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
        IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
        URI curLocation = null;
        for (RepositoryDescriptor repo : sourceRepositories) {
            try {
                curLocation = repo.getRepoLocation();
                if (repo.isBoth()) {
                    addRepository(artifactRepositoryManager, curLocation, 0, progress);
                    addRepository(metadataRepositoryManager, curLocation, 0, progress);
                } else if (repo.isArtifact())
                    addRepository(artifactRepositoryManager, curLocation, 0, progress);
                else if (repo.isMetadata())
                    addRepository(metadataRepositoryManager, curLocation, 0, progress);
                else
                    throw new ProvisionException(NLS.bind(Messages.unknown_repository_type, repo.getRepoLocation()));
            } catch (ProvisionException e) {
                if (e.getCause() instanceof MalformedURLException) {
                    throw new ProvisionException(NLS.bind(Messages.exception_invalidSource, curLocation), e);
                } else if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND && repo.isOptional()) {
                    continue;
                }
                throw e;
            }
        }
        processDestinationRepos(artifactRepositoryManager, metadataRepositoryManager);
    }

    // Helper to add a repository. It takes care of adding the repos to the deletion
    // list and loading it
    protected IMetadataRepository addRepository(IMetadataRepositoryManager manager, URI location, int flags,
            IProgressMonitor monitor) throws ProvisionException {
        if (!manager.contains(location))
            metadataReposToRemove.add(location);
        return manager.loadRepository(location, flags, monitor);
    }

    // Helper to add a repository. It takes care of adding the repos to the deletion
    // list and loading it
    protected IArtifactRepository addRepository(IArtifactRepositoryManager manager, URI location, int flags,
            IProgressMonitor monitor) throws ProvisionException {
        if (!manager.contains(location))
            artifactReposToRemove.add(location);
        return manager.loadRepository(location, flags, monitor);
    }

    private void processDestinationRepos(IArtifactRepositoryManager artifactRepositoryManager,
            IMetadataRepositoryManager metadataRepositoryManager) throws ProvisionException {
        RepositoryDescriptor artifactRepoDescriptor = null;
        RepositoryDescriptor metadataRepoDescriptor = null;

        Iterator<RepositoryDescriptor> iter = destinationRepos.iterator();
        while (iter.hasNext() && (artifactRepoDescriptor == null || metadataRepoDescriptor == null)) {
            RepositoryDescriptor repo = iter.next();
            if (repo.isArtifact() && artifactRepoDescriptor == null)
                artifactRepoDescriptor = repo;
            if (repo.isMetadata() && metadataRepoDescriptor == null)
                metadataRepoDescriptor = repo;
        }

        if (artifactRepoDescriptor != null)
            destinationArtifactRepository = initializeDestination(artifactRepoDescriptor, artifactRepositoryManager);
        if (metadataRepoDescriptor != null)
            destinationMetadataRepository = initializeDestination(metadataRepoDescriptor, metadataRepositoryManager);

        if (destinationMetadataRepository == null && destinationArtifactRepository == null)
            throw new ProvisionException(Messages.AbstractApplication_no_valid_destinations);
    }

    public IMetadataRepository getDestinationMetadataRepository() {
        return destinationMetadataRepository;
    }

    public IArtifactRepository getDestinationArtifactRepository() {
        return destinationArtifactRepository;
    }

    protected IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr)
            throws ProvisionException {
        try {
            IMetadataRepository repository = addRepository(mgr, toInit.getRepoLocation(),
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
            if (initDestinationRepository(repository, toInit))
                return repository;
        } catch (ProvisionException e) {
            // fall through and create a new repository below
        }

        IMetadataRepository source = null;
        try {
            if (toInit.getFormat() != null)
                source = mgr.loadRepository(toInit.getFormat(), 0, null);
        } catch (ProvisionException e) {
            // Ignore.
        }
        // This code assumes source has been successfully loaded before this point
        // No existing repository; create a new repository at destinationLocation but
        // with source's attributes.
        try {
            IMetadataRepository result = mgr.createRepository(toInit.getRepoLocation(),
                    toInit.getName() != null ? toInit.getName()
                            : (source != null ? source.getName() : toInit.getRepoLocation().toString()),
                    IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
            if (toInit.isCompressed() && !result.getProperties().containsKey(IRepository.PROP_COMPRESSED))
                result.setProperty(IRepository.PROP_COMPRESSED, "true"); //$NON-NLS-1$
            return (IMetadataRepository) RepositoryHelper.validDestinationRepository(result);
        } catch (UnsupportedOperationException e) {
            throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()),
                    e.getCause());
        }
    }

    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        try {
            IArtifactRepository repository = addRepository(mgr, toInit.getRepoLocation(),
                    IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
            if (initDestinationRepository(repository, toInit))
                return repository;
        } catch (ProvisionException e) {
            // fall through and create a new repository below
        }
        IArtifactRepository source = null;
        try {
            if (toInit.getFormat() != null)
                source = mgr.loadRepository(toInit.getFormat(), 0, null);
        } catch (ProvisionException e) {
            // Ignore.
        }
        // This code assumes source has been successfully loaded before this point
        // No existing repository; create a new repository at destinationLocation but
        // with source's attributes.
        // TODO for now create a Simple repo by default.
        try {
            IArtifactRepository result = mgr.createRepository(toInit.getRepoLocation(),
                    toInit.getName() != null ? toInit.getName()
                            : (source != null ? source.getName() : toInit.getRepoLocation().toString()),
                    IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, source != null ? source.getProperties() : null);
            if (toInit.isCompressed() && !result.getProperties().containsKey(IRepository.PROP_COMPRESSED))
                result.setProperty(IRepository.PROP_COMPRESSED, "true"); //$NON-NLS-1$
            return (IArtifactRepository) RepositoryHelper.validDestinationRepository(result);
        } catch (UnsupportedOperationException e) {
            throw new ProvisionException(NLS.bind(Messages.exception_invalidDestination, toInit.getRepoLocation()),
                    e.getCause());
        }
    }

    protected boolean initDestinationRepository(IRepository<?> repository, RepositoryDescriptor descriptor) {
        if (repository != null && repository.isModifiable()) {
            if (descriptor.getName() != null)
                repository.setProperty(IRepository.PROP_NAME, descriptor.getName());
            if (repository instanceof ICompositeRepository<?> && !descriptor.isAppend())
                ((ICompositeRepository<?>) repository).removeAllChildren();
            else if (repository instanceof IMetadataRepository && !descriptor.isAppend())
                ((IMetadataRepository) repository).removeAll();
            else if (repository instanceof IArtifactRepository && !descriptor.isAppend())
                ((IArtifactRepository) repository).removeAll();
            return true;
        }
        return false;
    }

    public synchronized IMetadataRepository getCompositeMetadataRepository() throws ProvisionException {
        if (compositeMetadataRepository == null) {
            IMetadataRepositoryManager repositoryManager = agent.getService(IMetadataRepositoryManager.class);
            List<IMetadataRepository> loadedRepository = new ArrayList<>();
            for (RepositoryDescriptor repo : sourceRepositories) {
                if (repo.isMetadata()) {
                    loadedRepository
                            .add(repositoryManager.loadRepository(repo.getRepoLocation(), new NullProgressMonitor()));
                }
            }
            compositeMetadataRepository = new ListCompositeMetadataRepository(loadedRepository, agent);
        }
        return compositeMetadataRepository;
    }

    public synchronized IArtifactRepository getCompositeArtifactRepository() throws ProvisionException {
        if (compositeArtifactRepository == null) {
            IArtifactRepositoryManager repositoryManager = agent.getService(IArtifactRepositoryManager.class);
            List<IArtifactRepository> loadedRepository = new ArrayList<>();
            for (RepositoryDescriptor repo : sourceRepositories) {
                if (repo.isArtifact()) {
                    loadedRepository
                            .add(repositoryManager.loadRepository(repo.getRepoLocation(), new NullProgressMonitor()));
                }
            }
            compositeArtifactRepository = new ListCompositeArtifactRepository(loadedRepository, agent);
        }
        return compositeArtifactRepository;
    }

    public boolean hasArtifactSources() throws ProvisionException {
        IArtifactRepository repository = getCompositeArtifactRepository();
        if (repository instanceof ICompositeRepository<?> composite) {
            return composite.getChildren().size() > 0;
        }
        return false;
    }

    public boolean hasMetadataSources() throws ProvisionException {
        IMetadataRepository repository = getCompositeMetadataRepository();
        if (repository instanceof ICompositeRepository<?> composite) {
            return composite.getChildren().size() > 0;
        }
        return false;
    }

    public abstract IStatus run(IProgressMonitor monitor) throws ProvisionException;

    public void addDestination(RepositoryDescriptor descriptor) {
        destinationRepos.add(descriptor);
    }

    public void addSource(RepositoryDescriptor repo) {
        sourceRepositories.add(repo);
    }
}
