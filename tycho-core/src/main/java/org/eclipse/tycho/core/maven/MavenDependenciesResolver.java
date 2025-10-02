/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;

public interface MavenDependenciesResolver {

    /**
     * Resolves the specified dependencies including their transitive ones.
     *
     * @param project
     *            The project whose dependencies should be resolved, must not be {@code null}.
     * @param dependencies
     * @param scopesToResolve
     *            The dependency scopes that should be resolved, may be {@code null}.
     * @param session
     *            The current build session, must not be {@code null}.
     * @return The transitive dependencies of the specified project that match the requested scopes,
     *         never {@code null}.
     * @throws DependencyCollectionException
     * @throws DependencyResolutionException
     */
    Collection<Artifact> resolve(MavenProject project, Collection<Dependency> dependencies,
            Collection<String> scopesToResolve, MavenSession session)
            throws DependencyCollectionException, DependencyResolutionException;

    /**
     * Resolves the highest version of the given dependency
     * 
     * @param project
     * @param session
     * @param dependency
     * @return
     * @throws VersionRangeResolutionException
     * @throws ArtifactResolutionException
     */
    Artifact resolveHighestVersion(MavenProject project, MavenSession session, Dependency dependency)
            throws VersionRangeResolutionException, ArtifactResolutionException;

    Artifact resolveArtifact(MavenProject project, MavenSession session, String groupId, String artifactId,
            String version) throws ArtifactResolutionException;

    Artifact resolveArtifact(MavenProject project, MavenSession session, Dependency dependency)
            throws ArtifactResolutionException;

}
