/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split target platform computation and dependency resolution
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.LoggingProgressMonitor;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.MavenMirrorRequest;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
public class P2ArtifactDownloadTool {

    private static final IArtifactRequest[] ARTIFACT_REQUEST_ARRAY = new IArtifactRequest[0];

    private final IProvisioningAgent agent;
    private final MavenLogger logger;

    public P2ArtifactDownloadTool(IProvisioningAgent agent, MavenLogger logger) {
        this.agent = agent;
        this.logger = logger;
    }

    public void downloadArtifactsToLocalMavenRepository(List<IArtifactKey> artifacts,
            List<URI> artifactRepositoryLocations, LocalArtifactRepository localMavenRepository,
            boolean includePackedArtifacts) {

        List<MavenMirrorRequest> requests = new ArrayList<MavenMirrorRequest>();
        for (IArtifactKey key : artifacts) {
            requests.add(new MavenMirrorRequest(key, localMavenRepository, getTransport(), includePackedArtifacts));
        }

        IArtifactRepository repository = createCompositeRepository(artifactRepositoryLocations);
        IStatus result = repository.getArtifacts(requests.toArray(ARTIFACT_REQUEST_ARRAY), new LoggingProgressMonitor(
                logger));
        if (!result.isOK()) {
            // TODO find root exception - the MultiStatus probably doesn't have one
            throw new RuntimeException(StatusTool.collectProblems(result), result.getException());
        }
        requests = filterCompletedRequests(requests);

        localMavenRepository.save();

        // check for locally installed artifacts, which are not available from any remote repo
        // TODO do this before downloading? (see enhancement request 342808)
        for (Iterator<MavenMirrorRequest> iter = requests.iterator(); iter.hasNext();) {
            MavenMirrorRequest request = iter.next();
            if (localMavenRepository.contains(request.getArtifactKey())) {
                iter.remove();
            }
        }

        if (!requests.isEmpty()) {
            StringBuilder msg = new StringBuilder("Could not download artifacts from any repository\n");
            for (MavenMirrorRequest request : requests) {
                msg.append("   ").append(request.getArtifactKey().toExternalForm()).append('\n');
            }

            throw new RuntimeException(msg.toString());
        }
    }

    private Transport getTransport() {
        return (Transport) agent.getService(Transport.SERVICE_NAME);
    }

    private IArtifactRepository createCompositeRepository(List<URI> artifactRepositories) {
        CompositeArtifactRepository composite = CompositeArtifactRepository.createMemoryComposite(agent);
        for (URI artifactRepository : artifactRepositories) {
            composite.addChild(artifactRepository);
        }
        return composite;
    }

    private List<MavenMirrorRequest> filterCompletedRequests(List<MavenMirrorRequest> requests) {
        ArrayList<MavenMirrorRequest> filteredRequests = new ArrayList<MavenMirrorRequest>();
        for (MavenMirrorRequest request : requests) {
            if (request.getResult() == null || !request.getResult().isOK()) {
                filteredRequests.add(request);
            }
        }
        return filteredRequests;
    }
}
