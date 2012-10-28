/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.repository;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
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
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProviderBaseImpl;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.formats.ArtifactTransferPolicy;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

public class RepositoryArtifactProvider extends CompositeArtifactProviderBaseImpl implements IRawArtifactProvider {

    static class RepositoryLoader {

        private IArtifactRepositoryManager repositoryManager;
        private List<URI> repositoryURLs;

        RepositoryLoader(List<URI> repositoryURLs, IProvisioningAgent agent) {
            this.repositoryURLs = repositoryURLs;
            this.repositoryManager = (IArtifactRepositoryManager) agent
                    .getService(IArtifactRepositoryManager.SERVICE_NAME);

            if (this.repositoryManager == null) {
                throw new IllegalArgumentException("IArtifactRepositoryManager in p2 agent " + agent);
            }
        }

        List<IArtifactRepository> loadRepositories() {
            List<IArtifactRepository> result = new ArrayList<IArtifactRepository>(repositoryURLs.size());
            for (URI repositoryURL : repositoryURLs) {
                try {
                    result.add(repositoryManager.loadRepository(repositoryURL, null));
                } catch (ProvisionException e) {
                    // don't ignore if repositories can't be loaded
                    // TODO improve message?
                    throw new RuntimeException(e);
                }
            }
            return result;
        }

        List<URI> getRepositoryURLs() {
            return repositoryURLs;
        }
    }

    private final RepositoryLoader repositoryLoader;
    List<IArtifactRepository> repositories;

    final ArtifactTransferPolicy transferPolicy;

    public RepositoryArtifactProvider(List<URI> artifactRepositories, ArtifactTransferPolicy transferPolicy,
            IProvisioningAgent agent) {
        this.repositoryLoader = new RepositoryLoader(artifactRepositories, agent);
        this.transferPolicy = transferPolicy;

    }

    // constructor for tests
    RepositoryArtifactProvider(RepositoryLoader repositoryLoader, ArtifactTransferPolicy transferPolicy) {
        this.repositoryLoader = repositoryLoader;
        this.transferPolicy = transferPolicy;
    }

    private void init() {
        if (repositories == null) {
            repositories = repositoryLoader.loadRepositories();
        }
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

    @Override
    protected void getArtifactDescriptorsOfAllSources(IArtifactKey key, Set<IArtifactDescriptor> result) {
        init();

        for (IArtifactRepository repository : repositories) {
            for (IArtifactDescriptor descriptor : repository.getArtifactDescriptors(key)) {
                result.add(descriptor);
            }
        }
    }

    @Override
    protected void getArtifactFromAnySource(IArtifactSink sink, List<IStatus> statusCollector, IProgressMonitor monitor)
            throws ArtifactSinkException {
        init();

        for (IArtifactRepository repository : repositories) {
            boolean artifactWasRead = getArtifactFromAnyFormatAvailableInRepository(repository, sink, statusCollector,
                    monitor);
            if (artifactWasRead) {
                return;
            }
        }
    }

    private boolean getArtifactFromAnyFormatAvailableInRepository(IArtifactRepository repository, IArtifactSink sink,
            List<IStatus> statusCollector, IProgressMonitor monitor) throws ArtifactSinkException {

        IArtifactDescriptor[] allFormats = repository.getArtifactDescriptors(sink.getArtifactToBeWritten());
        List<IArtifactDescriptor> formatsByPreference = transferPolicy.sortFormatsByPreference(allFormats);

        for (IArtifactDescriptor descriptor : formatsByPreference) {
            boolean artifactWasRead = getArtifactFromAnyMirror(descriptor, repository, sink, statusCollector, monitor);
            if (artifactWasRead) {
                return true;
            }
        }
        return false;
    }

    private boolean getArtifactFromAnyMirror(IArtifactDescriptor descriptor, IArtifactRepository repository,
            IArtifactSink sink, List<IStatus> statusCollector, IProgressMonitor monitor) throws ArtifactSinkException {

        // try at most a fixed number of mirrors; there is no API to query the actual number of mirrors
        for (int attemptIx = 0; attemptIx < 16; attemptIx++) {

            if (!sink.canBeginWrite()) {
                return false;
            }
            IStatus status = repository.getArtifact(descriptor, sink.beginWrite(), monitor);

            statusCollector.add(improveMessageIfError(status, repository, sink));
            if (isFatal(status)) {
                sink.abortWrite();

                // CODE_RETRY is how the repository signals that it has more mirrors to try
                if (status.getCode() != IArtifactRepository.CODE_RETRY) {
                    return false;
                }
            } else {
                sink.commitWrite();
                return true;
            }

        }
        return false;
    }

    @Override
    protected void getRawArtifactFromAnySource(IRawArtifactSink sink, IProgressMonitor monitor,
            List<IStatus> statusCollector) throws ArtifactSinkException {
        init();

        for (IArtifactRepository repository : repositories) {
            boolean artifactWasRead = getRawArtifactFromRepository(repository, sink, statusCollector, monitor);
            if (artifactWasRead) {
                return;
            }
        }
    }

    private boolean getRawArtifactFromRepository(IArtifactRepository repository, IRawArtifactSink sink,
            List<IStatus> statusCollector, IProgressMonitor monitor) throws ArtifactSinkException {
        IArtifactDescriptor requestedDescriptor = sink.getArtifactFormatToBeWritten();

        if (repository.contains(requestedDescriptor)) {
            if (!sink.canBeginWrite()) {
                return false;
            }
            IStatus status = repository.getRawArtifact(requestedDescriptor, sink.beginWrite(), monitor);

            statusCollector.add(improveMessageIfError(status, repository, requestedDescriptor));
            if (isFatal(status)) {
                sink.abortWrite();
            } else {
                sink.commitWrite();
                return true;
            }
        }
        return false;
    }

    private IStatus improveMessageIfError(IStatus originalStatus, IArtifactRepository repository,
            IArtifactDescriptor requestedDescriptor) {
        if (!isFatal(originalStatus)) {
            return originalStatus;
        }
        return improveErrorMessage(originalStatus, repository, requestedDescriptor.toString());
    }

    private IStatus improveMessageIfError(IStatus originalStatus, IArtifactRepository repository, IArtifactSink sink) {
        if (!isFatal(originalStatus)) {
            return originalStatus;
        }
        return improveErrorMessage(originalStatus, repository, sink.getArtifactToBeWritten().toString());
    }

    private IStatus improveErrorMessage(IStatus originalStatus, IArtifactRepository currentRepository,
            String currentArtifact) {
        String message = "An error occurred while transferring artifact " + currentArtifact + " from repository "
                + currentRepository.getLocation();
        return new MultiStatus(BUNDLE_ID, 0, new IStatus[] { originalStatus }, message, null);
    }

    @Override
    protected Status getArtifactNotFoundError(String artifact) {
        return new Status(IStatus.ERROR, BUNDLE_ID, ProvisionException.ARTIFACT_NOT_FOUND, "Artifact " + artifact
                + " is not available in any of the following repositories: " + repositoryLoader.getRepositoryURLs(),
                null);
    }

    public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
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
