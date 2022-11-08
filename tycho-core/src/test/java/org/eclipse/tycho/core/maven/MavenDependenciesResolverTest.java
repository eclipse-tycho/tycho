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
import org.apache.maven.execution.DefaultMavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.core.shared.DependencyResolutionException;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.osgi.configuration.MavenDependenciesResolverConfigurer;
import org.junit.Test;

public class MavenDependenciesResolverTest extends AbstractMojoTestCase {

    @Test
    public void testResolveCommonsIO() throws DependencyResolutionException, PlexusContainerException,
            ComponentLookupException, MavenExecutionRequestPopulationException, IOException {
        MavenSession session = newMavenSession(new MavenProject());
        File localRepo = Files.createTempDirectory("testResolveCommonsIO").toFile();
        session.getRequest().setLocalRepositoryPath(localRepo);
        DefaultMavenExecutionRequestPopulator populator = getContainer()
                .lookup(DefaultMavenExecutionRequestPopulator.class);
        populator.populateDefaults(session.getRequest());
        getContainer().lookup(LegacySupport.class).setSession(session);
        MavenDependenciesResolverConfigurer resolver = (MavenDependenciesResolverConfigurer) getContainer()
                .lookup(MavenDependenciesResolver.class);
        // the artifact must be pre-existing in the local repo, so the dep is added to pom.xml for this module. Test could be enhanced to fetch the artifact.
        Collection<?> deps = resolver.resolve("commons-io", "commons-io", "2.11.0", "jar", null, List.of(),
                Integer.MAX_VALUE, session.getRequest().getRemoteRepositories().stream()
                        .map(repo -> new MavenArtifactRepositoryReference() {

                            @Override
                            public String getUrl() {
                                return repo.getUrl();
                            }

                            @Override
                            public String getId() {
                                return repo.getId();
                            }
                        }).map(MavenArtifactRepositoryReference.class::cast) //
                        .toList(),
                session);
        assertEquals(deps.toString(), 1, deps.size());
        FileUtils.deleteDirectory(localRepo);
    }
}
