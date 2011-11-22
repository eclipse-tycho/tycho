/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

public class AbstractTychoMojoTestCase extends AbstractMojoTestCase {

    protected Maven maven;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        maven = lookup(Maven.class);
    }

    @Override
    protected void tearDown() throws Exception {
        maven = null;
        super.tearDown();
    }

    @Override
    protected String getCustomConfigurationName() {
        String name = AbstractTychoMojoTestCase.class.getName().replace('.', '/') + ".xml";
        return name;
    }

    protected ArtifactRepository getLocalRepository() throws Exception {
        RepositorySystem repoSystem = lookup(RepositorySystem.class);

        File path = new File("target/local-repo").getCanonicalFile();

        ArtifactRepository r = repoSystem.createLocalRepository(path);

        return r;
    }

    protected MavenExecutionRequest newMavenExecutionRequest(File pom) throws Exception {
        Properties systemProps = new Properties();
        systemProps.putAll(System.getProperties());

        Properties userProps = new Properties();
        userProps.put("tycho-version", "0.0.0");

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(pom.getParentFile());
        request.setPom(pom);
        request.setSystemProperties(systemProps);
        request.setUserProperties(userProps);
        request.setLocalRepository(getLocalRepository());

        request.setGoals(Arrays.asList("validate"));

        return request;
    }

    protected List<MavenProject> getSortedProjects(File basedir, File platform) throws Exception {
        return getSortedProjects(basedir, null, platform);
    }

    protected List<MavenProject> getSortedProjects(File basedir, Properties userProperties, File platform)
            throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        request.setLocalRepository(getLocalRepository());
        if (platform != null) {
            request.getUserProperties().put("tycho.targetPlatform", platform.getCanonicalPath());
        }
        if (userProperties != null) {
            request.getUserProperties().putAll(userProperties);
        }
        MavenExecutionResult result = maven.execute(request);
        if (result.hasExceptions()) {
            throw new CompoundRuntimeException(result.getExceptions());
        }
        return result.getTopologicallySortedProjects();
    }

    protected MavenSession newMavenSession(MavenProject project, List<MavenProject> projects) throws Exception {
        MavenExecutionRequest request = newMavenExecutionRequest(new File(project.getBasedir(), "pom.xml"));
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        MavenSession session = new MavenSession(getContainer(), repositorySession, request, result);
        session.setCurrentProject(project);
        session.setProjects(projects);
        return session;
    }

    protected MavenProject getProject(List<MavenProject> projects, String artifactId) {
        for (MavenProject project : projects) {
            if (artifactId.equals(project.getArtifactId())) {
                return project;
            }
        }

        throw new IllegalArgumentException("No project with artifactId " + artifactId);
    }

    protected static File getBasedir(String name) throws IOException {
        return TestUtil.getBasedir(name);
    }

}
