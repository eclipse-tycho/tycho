/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - reuse p2 data of POM dependencies if available (bug 342851)
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
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
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.maven.MavenArtifactFacade;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;

public class PomDependencyProcessor {

    private final MavenSession session;
    private final RepositorySystem repositorySystem;
    private final Logger logger;
    private P2ResolverFactory resolverFactory;
    private final LocalRepositoryP2Indices localRepoIndices;

    public PomDependencyProcessor(MavenSession session, RepositorySystem repositorySystem,
            P2ResolverFactory resolverFactory, LocalRepositoryP2Indices localRepoIndices, Logger logger) {
        this.session = session;
        this.repositorySystem = repositorySystem;
        this.logger = logger;
        this.resolverFactory = resolverFactory;
        this.localRepoIndices = localRepoIndices;
    }

    PomDependencyCollector collectPomDependencies(MavenProject project, Collection<Artifact> transitivePomDependencies,
            boolean allowGenerateOSGiBundle) {
        final TychoRepositoryIndex p2ArtifactsInLocalRepo = localRepoIndices.getArtifactsIndex();
        PomDependencyCollector result = resolverFactory.newPomDependencyCollector(DefaultReactorProject.adapt(project));

        for (Artifact artifact : transitivePomDependencies) {
            if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                // ignore
                continue;
            }
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

                result.addArtifactWithExistingMetadata(new ArtifactFacade(artifact),
                        new ArtifactFacade(p2Data.p2MetadataXml.artifact));

                /*
                 * Since the p2artifacts.xml exists on disk, we can add the artifact to the (global)
                 * p2 artifact repository view of local Maven repository. Then, the artifact is
                 * available in the build.
                 */
                // TODO this should happen in resolution context
                p2ArtifactsInLocalRepo
                        .addGav(new GAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion()));

            } else if (!p2Data.p2MetadataXml.isAvailable() && !p2Data.p2ArtifactsXml.isAvailable()) {
                /*
                 * The POM dependency has not been built by Tycho. If the dependency is a bundle,
                 * run the p2 bundle publisher on it and add the result to the resolution context.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("P2resolver.addMavenArtifact " + artifact.toString());
                }
                result.addMavenArtifact(new MavenArtifactFacade(artifact), allowGenerateOSGiBundle);
            } else {
                failDueToPartialP2Data(artifact, p2Data);
            }
        }
        return result;
    }

    private void failDueToPartialP2Data(Artifact artifact, P2DataArtifacts p2Data) {
        String p2MetadataFileName = TychoConstants.CLASSIFIER_P2_METADATA + "."
                + TychoConstants.EXTENSION_P2_METADATA;
        String p2ArtifactsFileName = TychoConstants.CLASSIFIER_P2_ARTIFACTS + "."
                + TychoConstants.EXTENSION_P2_ARTIFACTS;
        String artifactGAV = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        String message = "Only one of the p2 data artifacts " + p2MetadataFileName + "/" + p2ArtifactsFileName
                + " of the POM dependency " + artifactGAV + " could be resolved";

        throw new RuntimeException(message);
    }

    private class P2DataArtifacts {
        final ResolvableArtifact p2MetadataXml;
        final ResolvableArtifact p2ArtifactsXml;

        P2DataArtifacts(Artifact mainArtifact) {
            p2MetadataXml = getAttachedArtifactFor(mainArtifact, TychoConstants.CLASSIFIER_P2_METADATA,
                    TychoConstants.EXTENSION_P2_METADATA);
            p2ArtifactsXml = getAttachedArtifactFor(mainArtifact, TychoConstants.CLASSIFIER_P2_ARTIFACTS,
                    TychoConstants.EXTENSION_P2_ARTIFACTS);
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
