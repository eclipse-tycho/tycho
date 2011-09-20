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

import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;

public class PomDependencyProcessor {

    private final MavenSession session;
    private final RepositorySystem repositorySystem;
    private final Logger logger;
    private final LocalRepositoryP2Indices localRepoIndices;

    public PomDependencyProcessor(MavenSession session, RepositorySystem repositorySystem,
            LocalRepositoryP2Indices localRepoIndices, Logger logger) {
        this.session = session;
        this.repositorySystem = repositorySystem;
        this.logger = logger;
        this.localRepoIndices = localRepoIndices;
    }

    void addPomDependenciesToResolutionContext(MavenProject project, Collection<Artifact> transitivePomDependencies,
            ResolutionContext resolutionContext) {
        final TychoRepositoryIndex p2ArtifactsInLocalRepo = localRepoIndices.getArtifactsIndex();

        for (Artifact artifact : transitivePomDependencies) {
            P2DataArtifacts p2Data = new P2DataArtifacts(artifact);
            p2Data.resolve(session, project.getRemoteArtifactRepositories());

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

                resolutionContext.addArtifactWithExistingMetadata(new ArtifactFacade(artifact), new ArtifactFacade(
                        p2Data.p2MetadataXml.artifact));

                /*
                 * Since the p2artifacts.xml exists on disk, we can add the artifact to the (global)
                 * p2 artifact repository view of local Maven repository. Then, the artifact is
                 * available in the build.
                 */
                // TODO this should happen in resolution context
                p2ArtifactsInLocalRepo.addGav(new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact
                        .getVersion()));

            } else if (!p2Data.p2MetadataXml.isAvailable() && !p2Data.p2ArtifactsXml.isAvailable()) {
                /*
                 * The POM dependency has not been built by Tycho. If the dependency is a bundle,
                 * run the p2 bundle publisher on it and add the result to the resolution context.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("P2resolver.addMavenArtifact " + artifact.toString());
                }

                resolutionContext.publishAndAddArtifactIfBundleArtifact(new ArtifactFacade(artifact));

            } else {
                failDueToPartialP2Data(artifact, p2Data);
            }
        }
    }

    private void failDueToPartialP2Data(Artifact artifact, P2DataArtifacts p2Data) {
        String p2MetadataFileName = RepositoryLayoutHelper.CLASSIFIER_P2_METADATA + "."
                + RepositoryLayoutHelper.EXTENSION_P2_METADATA;
        String p2ArtifactsFileName = RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS + "."
                + RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS;
        String artifactGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        String message = "Only one of the p2 data artifacts " + p2MetadataFileName + "/" + p2ArtifactsFileName
                + " of the POM dependency " + artifactGAV + " could be resolved";

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

        void resolve(MavenSession session, List<ArtifactRepository> remoteMavenRepositories) {
            p2MetadataXml.resolve(repositorySystem, session, remoteMavenRepositories);
            p2ArtifactsXml.resolve(repositorySystem, session, remoteMavenRepositories);
        }

    }

    private static class ResolvableArtifact {
        final Artifact artifact;

        ResolvableArtifact(Artifact artifact) {
            this.artifact = artifact;
        }

        void resolve(RepositorySystem repositorySystem, MavenSession session,
                List<ArtifactRepository> remoteMavenRepositories) {
            session.getLocalRepository().find(artifact);
        }

        boolean isAvailable() {
            return artifact.getFile() != null && artifact.getFile().canRead();
        }
    }
}
