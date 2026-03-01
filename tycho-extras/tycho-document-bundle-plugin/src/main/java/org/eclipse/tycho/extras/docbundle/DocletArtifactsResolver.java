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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

@Named
@Singleton
public class DocletArtifactsResolver {

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private LegacySupport legacySupport;

    public DocletArtifactsResolver() {
        // needed for plexus
    }

    DocletArtifactsResolver(RepositorySystem repositorySystem, LegacySupport legacySupport) {
        this.repositorySystem = repositorySystem;
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
            String classifier = dependency.getClassifier();
            String extension = dependency.getType() != null ? dependency.getType() : "jar";
            org.eclipse.aether.artifact.Artifact aetherArtifact;
            if (classifier != null && !classifier.isEmpty()) {
                aetherArtifact = new DefaultArtifact(dependency.getGroupId(), 
                        dependency.getArtifactId(), classifier, extension, 
                        dependency.getVersion());
            } else {
                aetherArtifact = new DefaultArtifact(dependency.getGroupId(), 
                        dependency.getArtifactId(), null, extension, 
                        dependency.getVersion());
            }
            
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(aetherArtifact, null));
            collectRequest.setRepositories(RepositoryUtils.toRepos(project.getPluginArtifactRepositories()));
            
            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
            
            try {
                DependencyResult result = repositorySystem.resolveDependencies(session.getRepositorySession(), dependencyRequest);
                for (var ar : result.getArtifactResults()) {
                    if (ar.isResolved()) {
                        org.eclipse.aether.artifact.Artifact resolved = ar.getArtifact();
                        if (resolved != null && resolved.getFile() != null) {
                            files.add(resolved.getFile().getAbsolutePath());
                        }
                    }
                }
            } catch (DependencyResolutionException e) {
                throw new MojoExecutionException("Failed to resolve doclet artifact " + dependency.getManagementKey(), e);
            }
        }

        return files;
    }
}
