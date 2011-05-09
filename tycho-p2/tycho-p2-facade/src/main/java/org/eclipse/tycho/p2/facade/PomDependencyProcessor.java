/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - reuse p2 data of POM dependencies if available (bug 342851)
 *******************************************************************************/
package org.eclipse.tycho.p2.facade;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.resolver.P2Resolver;

public class PomDependencyProcessor {

    private final MavenSession session;
    private final RepositorySystem repositorySystem;
    private final Logger logger;

    public PomDependencyProcessor(MavenSession session, RepositorySystem repositorySystem, Logger logger) {
        this.session = session;
        this.repositorySystem = repositorySystem;
        this.logger = logger;
    }

    void addPomDependenciesToResolutionContext(MavenProject project, Collection<Artifact> transitivePomDependencies,
            P2Resolver resolutionContext) {
        final LocalTychoRepositoryIndex artifactsIndex = new LocalTychoRepositoryIndex(new File(session
                .getLocalRepository().getBasedir()), LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
        final List<ArtifactRepository> remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        for (Artifact artifact : transitivePomDependencies) {
            final Artifact p2ArtifactData = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS,
                    RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS);
            final ArtifactResolutionResult result = resolveArtifact(p2ArtifactData, remoteArtifactRepositories);
            if (result.isSuccess()) {
                // add to .meta/localArtifacts.properties
                artifactsIndex.addProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            } else {
                // TODO as part of TYCHO-570: generate p2 artifact entry
            }
        }

        try {
            artifactsIndex.save();
        } catch (IOException e) {
            throw new RuntimeException("I/O error while updating p2 view on local Maven repository", e);
        }

        for (Artifact artifact : transitivePomDependencies) {
            final Artifact p2MetadataData = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), RepositoryLayoutHelper.EXTENSION_P2_METADATA,
                    RepositoryLayoutHelper.CLASSIFIER_P2_METADATA);
            final ArtifactResolutionResult result = resolveArtifact(p2MetadataData, remoteArtifactRepositories);
            if (result.isSuccess()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("P2TargetPlatformResolver: Using existing metadata of " + artifact.toString());
                }
                resolutionContext.addTychoArtifact(new ArtifactFacade(artifact), new ArtifactFacade(p2MetadataData));
            } else {
                /*
                 * TODO The generated metadata is "depencency only" metadata. (Just by coincidence
                 * this is currently full metadata.) Since this POM depencency metadata may be
                 * copied into an eclipse-repository or p2-enabled RCP installation, it shall be
                 * documented that the generated metadata must be full metadata.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("P2resolver.addMavenArtifact " + artifact.toString());
                }
                resolutionContext.addMavenArtifact(new ArtifactFacade(artifact));
            }
        }

    }

    private ArtifactResolutionResult resolveArtifact(Artifact artifact, List<ArtifactRepository> repositories) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setRemoteRepositories(repositories);
        return repositorySystem.resolve(request);
    }
}
