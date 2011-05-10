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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.p2.facade.RepositoryReferenceTool;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.repository.LocalTychoRepositoryIndex;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;

public class PomDependencyProcessor {

    private final MavenSession session;
    private final RepositorySystem repositorySystem;
    private final Logger logger;
    private final ResolutionErrorHandler resolutionErrorHelper;

    public PomDependencyProcessor(MavenSession session, RepositorySystem repositorySystem, Logger logger,
            ResolutionErrorHandler resolutionErrorHelper) {
        this.session = session;
        this.repositorySystem = repositorySystem;
        this.logger = logger;
        this.resolutionErrorHelper = resolutionErrorHelper;
    }

    void addPomDependenciesToResolutionContext(MavenProject project, Collection<Artifact> transitivePomDependencies,
            P2Resolver resolutionContext) {
        final LocalTychoRepositoryIndex p2ArtifactsInLocalRepo = loadIndexOfP2ArtifactsInLocalMavenRepo();

        for (Artifact artifact : transitivePomDependencies) {
            P2DataArtifacts p2Data = new P2DataArtifacts(artifact);
            p2Data.attemptDownload(project.getRemoteArtifactRepositories());

            if (p2Data.p2MetadataXml.isAvailable() && p2Data.p2ArtifactsXml.isAvailable()) {
                /*
                 * The POM dependency has (probably) been built by Tycho, so we can re-use the
                 * existing p2 data in the target platform. The data is stored in the attached
                 * artifacts p2metadata.xml and p2artifacts.xml, which are now present in the local
                 * Maven repository.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("P2TargetPlatformResolver: Using existing metadata of " + artifact.toString());
                }

                addPomDependencyWithExistingP2Metadata(artifact, p2Data.p2MetadataXml.artifact, resolutionContext);

                /*
                 * Since the p2artifacts.xml exists on disk, we can add the artifact to the (global)
                 * p2 artifact repository view of local Maven repository. Then, the artifact is
                 * available in the build.
                 */
                p2ArtifactsInLocalRepo.addProject(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getVersion());

            } else if (!p2Data.p2MetadataXml.isAvailable() && !p2Data.p2ArtifactsXml.isAvailable()) {
                /*
                 * The POM dependency has not been built by Tycho. If the dependency is a bundle,
                 * run the p2 bundle publisher on it and add the result to the resolution context.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("P2resolver.addMavenArtifact " + artifact.toString());
                }

                /*
                 * TODO The generated metadata is "depencency only" metadata. (Just by coincidence
                 * this is currently full metadata.) Since this POM depencency metadata may be
                 * copied into an eclipse-repository or p2-enabled RCP installation, it shall be
                 * documented that the generated metadata must be full metadata.
                 */
                // TODO move metadata generation out of the p2 resolver
                resolutionContext.addMavenArtifact(new ArtifactFacade(artifact));

                // TODO as part of TYCHO-570: generate and collect p2 artifact entry

            } else {
                failDueToPartialP2Data(artifact, p2Data);
            }
        }

        try {
            p2ArtifactsInLocalRepo.save();
        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error while updating p2 artifact repository view on local Maven repository", e);
        }
    }

    private void addPomDependencyWithExistingP2Metadata(Artifact artifact, Artifact p2MetadataArtifact,
            P2Resolver resolutionContext) {
        resolutionContext.addTychoArtifact(new ArtifactFacade(artifact), new ArtifactFacade(p2MetadataArtifact));
    }

    /**
     * Loads the list of artifacts that are contained in the p2 artifact repository view of the
     * local Maven repository. This list is stored in the local Maven repository under
     * .meta/localArtifacts.properties
     * 
     * @see RepositoryReferenceTool#getVisibleRepositories(MavenProject, MavenSession, int)
     * @see org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository
     */
    private LocalTychoRepositoryIndex loadIndexOfP2ArtifactsInLocalMavenRepo() {
        File localMavenRepository = new File(session.getLocalRepository().getBasedir());
        return new LocalTychoRepositoryIndex(localMavenRepository, LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH);
    }

    private void failDueToPartialP2Data(Artifact artifact, P2DataArtifacts p2Data) {
        String p2MetadataFileName = RepositoryLayoutHelper.CLASSIFIER_P2_METADATA + "."
                + RepositoryLayoutHelper.EXTENSION_P2_METADATA;
        String p2ArtifactsFileName = RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS + "."
                + RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS;
        String artifactGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        String message = "Only one of the p2 data artifacts " + p2MetadataFileName + "/" + p2ArtifactsFileName
                + " of the POM dependency " + artifactGAV + " could be resolved";

        try {
            resolutionErrorHelper.throwErrors(p2Data.p2MetadataXml.resolutionRequest,
                    p2Data.p2MetadataXml.resolutionResult);
            resolutionErrorHelper.throwErrors(p2Data.p2ArtifactsXml.resolutionRequest,
                    p2Data.p2ArtifactsXml.resolutionResult);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(message, e);
        }
        throw new RuntimeException(message);
    }

    private class P2DataArtifacts {
        final ResolvableArtifact p2MetadataXml;
        final ResolvableArtifact p2ArtifactsXml;

        P2DataArtifacts(Artifact mainArtifact) {
            p2MetadataXml = getAttachedArtifactFor(mainArtifact, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                    RepositoryLayoutHelper.EXTENSION_P2_METADATA);
            p2ArtifactsXml = getAttachedArtifactFor(mainArtifact, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                    RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS);
        }

        private ResolvableArtifact getAttachedArtifactFor(Artifact mainArtifact, String classifier, String extension) {
            Artifact artifact = repositorySystem.createArtifactWithClassifier(mainArtifact.getGroupId(),
                    mainArtifact.getArtifactId(), mainArtifact.getVersion(), extension, classifier);
            return new ResolvableArtifact(artifact);
        }

        void attemptDownload(List<ArtifactRepository> remoteMavenRepositories) {
            p2MetadataXml.resolve(repositorySystem, remoteMavenRepositories);
            p2ArtifactsXml.resolve(repositorySystem, remoteMavenRepositories);
        }

    }

    private static class ResolvableArtifact {
        final Artifact artifact;
        ArtifactResolutionRequest resolutionRequest = null;
        ArtifactResolutionResult resolutionResult = null;

        ResolvableArtifact(Artifact artifact) {
            this.artifact = artifact;
        }

        void resolve(RepositorySystem repositorySystem, List<ArtifactRepository> remoteMavenRepositories) {
            resolutionRequest = new ArtifactResolutionRequest();
            resolutionRequest.setArtifact(artifact);
            resolutionRequest.setRemoteRepositories(remoteMavenRepositories);
            resolutionResult = repositorySystem.resolve(resolutionRequest);
        }

        boolean isAvailable() {
            return resolutionResult.isSuccess();
        }
    }
}
