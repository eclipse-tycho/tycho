/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = MavenArtifactResolver.class)
public class MavenArtifactResolver {

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    // TODO reuse implementation in DocletArtifactResolver?
    public File getPluginArtifact(Dependency reference, MavenSession session) throws MojoFailureException,
            MojoExecutionException {
        MavenProject project = session.getCurrentProject();

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(repositorySystem.createDependencyArtifact(reference));
        request.setResolveRoot(true)/* .setResolveTransitively(true) */;
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(project.getPluginArtifactRepositories());
        request.setOffline(session.isOffline());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());
        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);
        try {
            resolutionErrorHandler.throwErrors(request, resolutionResult);
        } catch (ArtifactResolutionException e) {
            throw new MojoFailureException("Failed to resolve build plugin artifact " + reference.getManagementKey(), e);
        }

        Set<Artifact> resolvedArtifacts = resolutionResult.getArtifacts();
        if (resolvedArtifacts.size() > 1) {
            throw new MojoExecutionException(
                    "Expected only one artifact from build plugin artifact resolution, but was " + resolvedArtifacts);
        }
        return resolvedArtifacts.iterator().next().getFile().getAbsoluteFile();
    }

}
