/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bachmann electronic GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

@Named
@Singleton
public class DocletArtifactsResolver {

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private ResolutionErrorHandler resolutionErrorHandler;

    @Inject
    private LegacySupport legacySupport;

    public DocletArtifactsResolver() {
        // needed for plexus
    }

    DocletArtifactsResolver(RepositorySystem repositorySystem, ResolutionErrorHandler resolutionErrorHandler,
            LegacySupport legacySupport) {
        this.repositorySystem = repositorySystem;
        this.resolutionErrorHandler = resolutionErrorHandler;
        this.legacySupport = legacySupport;
    }

    /**
     * Resolves the dependencies and returns a list of paths to the dependency jar files.
     * 
     * @param dependencies
     *            the Dependencies to be resolved
     * @return the paths to the jar files
     * @throws MojoExecutionException
     *             if one of the specified depenencies could not be resolved
     */
    public Set<String> resolveArtifacts(List<Dependency> dependencies) throws MojoExecutionException {
        Set<String> files = new LinkedHashSet<>();

        if (dependencies == null || dependencies.isEmpty()) {
            return files;
        }

        MavenSession session = legacySupport.getSession();
        MavenProject project = session.getCurrentProject();

        for (Dependency dependency : dependencies) {
            Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(artifact);
            request.setResolveRoot(true).setResolveTransitively(true);
            request.setLocalRepository(session.getLocalRepository());
            request.setRemoteRepositories(project.getPluginArtifactRepositories());
            request.setOffline(session.isOffline());
            request.setForceUpdate(session.getRequest().isUpdateSnapshots());
            ArtifactResolutionResult result = repositorySystem.resolve(request);
            try {
                resolutionErrorHandler.throwErrors(request, result);
            } catch (ArtifactResolutionException e) {
                throw new MojoExecutionException("Failed to resolve doclet artifact " + dependency.getManagementKey(),
                        e);
            }
            for (Artifact resolvedArtifact : result.getArtifacts()) {
                files.add(resolvedArtifact.getFile().getAbsolutePath());
            }
        }

        return files;
    }
}
