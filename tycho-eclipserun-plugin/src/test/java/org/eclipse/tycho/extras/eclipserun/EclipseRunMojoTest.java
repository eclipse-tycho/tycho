/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.extras.eclipserun;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class EclipseRunMojoTest extends AbstractTychoMojoTestCase {

    private EclipseRunMojo runMojo;
    private EquinoxInstallation installation;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        runMojo = new EclipseRunMojo();
        installation = mock(EquinoxInstallation.class);
        MavenProject project = mock(MavenProject.class);
        setVariableValueToObject(runMojo, "project", project);
        ToolchainProvider toolchainProvider = mock(ToolchainProvider.class);
        setVariableValueToObject(runMojo, "toolchainProvider", toolchainProvider);
        when(installation.getLocation()).thenReturn(new File("installpath"));
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
}
