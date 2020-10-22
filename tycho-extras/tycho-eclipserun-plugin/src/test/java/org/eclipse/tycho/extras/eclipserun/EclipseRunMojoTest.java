/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.extras.eclipserun;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.rules.TemporaryFolder;

public class EclipseRunMojoTest extends AbstractTychoMojoTestCase {

    private EclipseRunMojo runMojo;
    private EquinoxInstallation installation;
    private TemporaryFolder temporaryFolder;
    private File workFolder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        runMojo = new EclipseRunMojo() {
            @Override
            public Log getLog() {
                return new SilentLog();
            }
        };
        installation = mock(EquinoxInstallation.class);
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        MavenProject project = mock(MavenProject.class);
        setVariableValueToObject(runMojo, "project", project);
        ToolchainProvider toolchainProvider = mock(ToolchainProvider.class);
        setVariableValueToObject(runMojo, "toolchainProvider", toolchainProvider);
        workFolder = new File(temporaryFolder.getRoot(), "work");
        setVariableValueToObject(runMojo, "work", workFolder);
        setVariableValueToObject(runMojo, "launcher", mock(EquinoxLauncher.class));
        when(installation.getLocation()).thenReturn(new File("installpath"));
    }

    @Override
    protected void tearDown() throws Exception {
        temporaryFolder.delete();
        super.tearDown();
    }

    public void testCreateCommandlineWithJvmArgs()
            throws IllegalAccessException, MalformedURLException, MojoExecutionException {
        List<String> args = Arrays.asList("-Xdebug", "-DanotherOptionWithValue=theValue",
                "-DoptionWith=\"A space in the value\"");
        setVariableValueToObject(runMojo, "jvmArgs", args);
        setVariableValueToObject(runMojo, "argLine", "-DoldArgLineOption");
        LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
        List<String> vmArgs = Arrays.asList(commandLine.getVMArguments());
        assertTrue(vmArgs.contains("-Xdebug"));
        assertTrue(vmArgs.contains("-DanotherOptionWithValue=theValue"));
        assertTrue(vmArgs.contains("-DoldArgLineOption"));
        assertTrue(vmArgs.contains("-DoptionWith=\"A space in the value\""));
    }

    public void testCreateCommandLineWithNullJvmArgs() throws MalformedURLException, MojoExecutionException {
        LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
        assertTrue(commandLine.getVMArguments().length == 0);
    }

    public void testCreateCommandLineProgramArgs() throws MalformedURLException, MojoExecutionException {
        LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
        List<String> programArgs = Arrays.asList(commandLine.getProgramArguments());
        assertThat(programArgs, contains( //
                "-install", installation.getLocation().getAbsolutePath(), //
                "-configuration", new File(workFolder, "configuration").getAbsolutePath(), //
                "-data", new File(workFolder, "data").getAbsolutePath() //
        ));
    }

    public void testDataDirectoryIsClearedBeforeLaunching()
            throws IOException, MojoExecutionException, MojoFailureException {
        File markerFile = new File(workFolder, "data/markerfile").getAbsoluteFile();
        markerFile.getParentFile().mkdirs();
        markerFile.createNewFile();
        assertThat(markerFile.exists(), is(true));
        runMojo.runEclipse(installation);
        assertThat(markerFile.exists(), is(false));
    }
}
