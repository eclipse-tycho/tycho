/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequestPopulator;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.core.DependencyResolutionException;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.osgi.configuration.MavenDependenciesResolverConfigurer;
import org.junit.Test;

public class MavenDependenciesResolverTest extends AbstractMojoTestCase {

    @Test
    public void testResolveCommonsIO() throws DependencyResolutionException, PlexusContainerException,
            ComponentLookupException, MavenExecutionRequestPopulationException, IOException, Exception {
        File localRepo = Files.createTempDirectory("testResolveCommonsIO").toFile();
        
        // Create the Maven execution request
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepositoryPath(localRepo);
        
        DefaultMavenExecutionRequestPopulator populator = getContainer()
                .lookup(DefaultMavenExecutionRequestPopulator.class);
        populator.populateDefaults(request);
        
        // Create a properly configured repository session using LegacyLocalRepositoryManager
        DefaultRepositorySystemSession baseSession = MavenRepositorySystemUtils.newSession();
        ArtifactRepository localRepository = request.getLocalRepository();
        RepositorySystemSession repositorySession = LegacyLocalRepositoryManager.overlay(localRepository, baseSession, null);
        
        // Create session with proper repository system session
        MavenSession session = new MavenSession(getContainer(), repositorySession, request, 
                new DefaultMavenExecutionResult());
        session.setCurrentProject(new MavenProject());
        
        getContainer().lookup(LegacySupport.class).setSession(session);
        MavenDependenciesResolverConfigurer resolver = (MavenDependenciesResolverConfigurer) getContainer()
                .lookup(MavenDependenciesResolver.class);
        
        // Get the remote repositories from the request
        List<ArtifactRepository> remoteRepos = request.getRemoteRepositories();
        
        Collection<?> deps = resolver.resolve("commons-io", "commons-io", "2.11.0", "jar", null, List.of(),
                Integer.MAX_VALUE, remoteRepos.stream()
                        .map(repo -> new MavenArtifactRepositoryReference() {

                            @Override
                            public String getUrl() {
                                return repo.getUrl();
                            }

                            @Override
                            public String getId() {
                                return repo.getId();
                            }
                        }).map(MavenArtifactRepositoryReference.class::cast)
                        .toList(),
                session);
        assertEquals(deps.toString(), 1, deps.size());
        FileUtils.deleteDirectory(localRepo);
    }
}
