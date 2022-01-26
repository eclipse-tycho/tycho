/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;

public class AbstractTychoMojoTestCase extends AbstractMojoTestCase {

    protected Maven maven;
    private MavenSettingsBuilder settingsBuilder;
    private MavenExecutionRequestPopulator requestPopulator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        maven = lookup(Maven.class);
        settingsBuilder = lookup(MavenSettingsBuilder.class);
        requestPopulator = lookup(MavenExecutionRequestPopulator.class);
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

        File path = new File("target/local-repo").getAbsoluteFile();

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
        request.setStartTime(new Date());
        File settingsFile = getUserSettingsFile();
        if (settingsFile.isFile()) {
            request.setUserSettingsFile(settingsFile);
        }
        Settings settings = settingsBuilder.buildSettings(request);
        requestPopulator.populateFromSettings(request, settings);
        request.setGoals(Arrays.asList("validate"));
        request.setLocalRepositoryPath(getLocalRepository().getBasedir());
        return request;
    }

    private File getUserSettingsFile() throws IOException {
        String systemValue = System.getProperty("tycho.testSettings");
        if (systemValue != null) {
            return new File(systemValue);
        }
        Properties props = new Properties();
        try (InputStream stream = AbstractTychoMojoTestCase.class.getResourceAsStream("settings.properties")) {
            props.load(stream);
        }
        String settingsFilePath = props.getProperty("settings.file");
        return new File(settingsFilePath);
    }

    protected List<MavenProject> getSortedProjects(File basedir) throws Exception {
        return getSortedProjects(basedir, null, null);
    }

    @Deprecated
    protected List<MavenProject> getSortedProjects(File basedir, File platform) throws Exception {
        return getSortedProjects(basedir, null, platform);
    }

    protected List<MavenProject> getSortedProjects(File basedir, Properties userProperties) throws Exception {
        return getSortedProjects(basedir, userProperties, null);
    }

    @Deprecated
    protected List<MavenProject> getSortedProjects(File basedir, Properties userProperties, File platform)
            throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        request.setLocalRepository(getLocalRepository());
        if (platform != null) {
            request.getUserProperties().put("tycho.test.targetPlatform", platform.getAbsolutePath());
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
        session.setProjects(projects);
        session.setCurrentProject(project);
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

    /**
     * Returns a mojo configured with the mojo's default configuration.
     */
    // workaround for MPLUGINTESTING-46 - see http://jira.codehaus.org/browse/MPLUGINTESTING-46
    protected Mojo lookupMojoWithDefaultConfiguration(MavenProject project, MavenSession session, String goal)
            throws Exception {
        Mojo mojo = lookupEmptyMojo(goal, project.getFile());
        configureMojoWithDefaultConfiguration(mojo, session, goal);
        return mojo;
    }

    /**
     * Configures the given mojo according to the specified goal of the given session.
     * <p>
     * Especially this also initializes each {@link Parameter} of the mojo with its default values.
     * </p>
     */
    protected void configureMojoWithDefaultConfiguration(Mojo mojo, MavenSession session, String goal)
            throws Exception {
        MojoExecution mojoExecution = newMojoExecution(goal);
        configureMojoWithDefaultConfiguration(mojo, session, mojoExecution);
    }

    /**
     * Configures the given mojo according to the specified session and mojo-exectuion.
     * <p>
     * Especially this also initializes each {@link Parameter} of the mojo with its default values.
     * </p>
     */
    private void configureMojoWithDefaultConfiguration(Mojo mojo, MavenSession session, MojoExecution mojoExecution)
            throws Exception {
        Xpp3Dom defaultConfiguration = mojoExecution.getConfiguration();

        // the ResolverExpressionEvaluatorStub of lookupMojo is not sufficient to evaluate the variables in the default configuration 
        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        ComponentConfigurator configurator = getContainer().lookup(ComponentConfigurator.class, "basic");

        configurator.configureComponent(mojo, new XmlPlexusConfiguration(defaultConfiguration), expressionEvaluator,
                getContainer().getContainerRealm(), null);
    }

    protected static File getBasedir(String name) throws IOException {
        return TestUtil.getBasedir(name);
    }

}
