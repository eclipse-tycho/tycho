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
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
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
        this(new RepositoryLoader(artifactRepositories, agent), transferPolicy);

    }

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

        return getArtifactFromAnyChildRepository(formatsByPreference, repository, sink, statusCollector, monitor);
    }

    private boolean getArtifactFromAnyChildRepository(final List<IArtifactDescriptor> availableDescriptors,
            IArtifactRepository repository, final IArtifactSink sink, final List<IStatus> statusCollector,
            IProgressMonitor monitor) throws ArtifactSinkException {

        /**
         * Composite p2 repositories will execute this request for each child repository which
         * contain the artifact key (until successful). Using the getArtifacts method instead of
         * getArtifact directly prevents that composite repositories mark their children as bad when
         * the transfer of a broken pack200 artifact fails (cf. bug 412945).
         */
        BooleanStatusArtifactRequest request = new BooleanStatusArtifactRequest(sink.getArtifactToBeWritten()) {
            private final RetryTracker retryTracker = new RetryTracker();

            public void perform(IArtifactRepository childRepository, IProgressMonitor monitor) {
                try {
                    boolean artifactWasRead = getArtifactFromAnyMirror(availableDescriptors, childRepository, sink,
                            statusCollector, retryTracker, monitor);
                    if (artifactWasRead) {
                        this.markSuccessful();
                    }
                } catch (ArtifactSinkException e) {
                    throw new ArtifactSinkExceptionWrapper(e);
                }
            }
        };

        try {
            repository.getArtifacts(new IArtifactRequest[] { request }, monitor);
        } catch (ArtifactSinkExceptionWrapper e) {
            throw e.getWrappedException();
        }

        return request.wasSuccessful();
    }

    private boolean getArtifactFromAnyMirror(final List<IArtifactDescriptor> availableDescriptors,
            IArtifactRepository repository, final IArtifactSink sink, final List<IStatus> statusCollector,
            RetryTracker retryTracker, IProgressMonitor monitor) throws ArtifactSinkException {

        for (; retryTracker.canRetry(); retryTracker.increment()) {
            boolean artifactWasRead = getArtifactFromOneMirror(availableDescriptors, repository, sink, statusCollector,
                    retryTracker, monitor);
            if (artifactWasRead)
                return true;
        }
        return false;
    }

    private boolean getArtifactFromOneMirror(List<IArtifactDescriptor> availableDescriptors,
            IArtifactRepository repository, IArtifactSink sink, List<IStatus> statusCollector,
            RetryTracker retryTracker, IProgressMonitor monitor) throws ArtifactSinkException {

        for (IArtifactDescriptor descriptor : availableDescriptors) {
            if (!sink.canBeginWrite()) {
                return false;
            }
            // there is no way to explicitly select a mirror - the repository magically picks one
            IStatus status = repository.getArtifact(descriptor, sink.beginWrite(), monitor);

            statusCollector.add(improveMessageIfError(status, repository, descriptor));
            if (isFatal(status)) {
                sink.abortWrite();

                /*
                 * CODE_RETRY is how the repository signals that it has more mirrors to try, and
                 * that we can call the same method with exactly the same parameters (!) again.
                 * However we try another format first, so that we don't "spoil" all mirrors by
                 * continuing to querying for a pack200 artifact that is actually broken in the
                 * master repository (cf. bug 412945).
                 */
                if (status.getCode() != IArtifactRepository.CODE_RETRY) {
                    retryTracker.noMoreRetries();
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

    // TODO 393004 retry mirrors
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
            IArtifactDescriptor currentDescriptor) {
        if (!isFatal(originalStatus)) {
            return originalStatus;
        }
        String message = "An error occurred while transferring artifact " + currentDescriptor + " from repository "
                + repository.getLocation();
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

    private static class RetryTracker {
        // try at most a fixed number of mirrors; there is no API to query the actual number of mirrors
        private int remaining = 16;

        boolean canRetry() {
            return remaining > 0;
        }

        void increment() {
            --remaining;
        }

        void noMoreRetries() {
            remaining = 0;
        }
    }

    private static abstract class BooleanStatusArtifactRequest implements IArtifactRequest {
        private final IArtifactKey key;
        private boolean successful = false;

        public BooleanStatusArtifactRequest(IArtifactKey key) {
            this.key = key;
        }

        public final IArtifactKey getArtifactKey() {
            return key;
        }

        protected void markSuccessful() {
            successful = true;
        }

        public boolean wasSuccessful() {
            return successful;
        }

        public IStatus getResult() {
            if (successful) {
                return Status.OK_STATUS;
            } else {
                // actual status messages are tracked separately
                return new Status(IStatus.ERROR, BUNDLE_ID, "failure marker");
            }
        }

    }

    @SuppressWarnings("serial")
    private static class ArtifactSinkExceptionWrapper extends RuntimeException {
        private ArtifactSinkException wrappedException;

        public ArtifactSinkExceptionWrapper(ArtifactSinkException wrappedException) {
            this.wrappedException = wrappedException;
        }

        public ArtifactSinkException getWrappedException() {
            return wrappedException;
        }
    }

}
