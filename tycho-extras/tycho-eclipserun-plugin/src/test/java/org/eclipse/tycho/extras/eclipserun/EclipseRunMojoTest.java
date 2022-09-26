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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.LaunchConfiguration;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.rules.TemporaryFolder;

public class EclipseRunMojoTest extends AbstractTychoMojoTestCase {

	private EclipseRunMojo runMojo;
	private EquinoxInstallation installation;
	private TemporaryFolder temporaryFolder;
	private File workFolder;
	private ToolchainProvider toolchainProvider;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		runMojo = (EclipseRunMojo) lookupMojo("org.eclipse.tycho.extras", "tycho-eclipserun-plugin",
				TychoVersion.getTychoVersion(), "eclipse-run", null);
		runMojo.setLog(new SilentLog());
		MavenSession mavenSession = newMavenSession(mock(MavenProject.class));
		configureMojoWithDefaultConfiguration(runMojo, mavenSession, "eclipse-run");

		installation = mock(EquinoxInstallation.class);
		temporaryFolder = new TemporaryFolder();
		temporaryFolder.create();
		MavenProject project = mock(MavenProject.class);
		setVariableValueToObject(runMojo, "project", project);
		toolchainProvider = mock(ToolchainProvider.class);
		setVariableValueToObject(runMojo, "toolchainProvider", toolchainProvider);
		workFolder = new File(temporaryFolder.getRoot(), "work");
		setVariableValueToObject(runMojo, "work", workFolder);
		setVariableValueToObject(runMojo, "launcher", mock(EquinoxLauncher.class));
		setVariableValueToObject(runMojo, "repositories", List.of());
		when(installation.getLocation()).thenReturn(new File("installpath"));
	}

	@Override
	protected void tearDown() throws Exception {
		temporaryFolder.delete();
		super.tearDown();
	}

	public void testCreateCommandlineWithJvmArgs() throws IllegalAccessException, MojoExecutionException {
		List<String> args = Arrays.asList("-Xdebug", "-DanotherOptionWithValue=theValue",
				"-DoptionWith=\"A space in the value\"");
		setVariableValueToObject(runMojo, "jvmArgs", args);
		LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
		List<String> vmArgs = Arrays.asList(commandLine.getVMArguments());
		assertTrue(vmArgs.contains("-Xdebug"));
		assertTrue(vmArgs.contains("-DanotherOptionWithValue=theValue"));
		assertTrue(vmArgs.contains("-DoptionWith=\"A space in the value\""));
	}

	public void testCreateCommandlineWithApplicationArgs() throws IllegalAccessException, MojoExecutionException {
		List<String> args = Arrays.asList("arg1", "literal arg with spaces", "argument'with'literalquotes");
		setVariableValueToObject(runMojo, "applicationArgs", args);
		LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
		List<String> programArgs = Arrays.asList(commandLine.getProgramArguments());
		assertTrue(programArgs.contains("arg1"));
		assertTrue(programArgs.contains("literal arg with spaces"));
		assertTrue(programArgs.contains("argument'with'literalquotes"));
	}

	public void testCreateCommandLineWithNullJvmArgs() throws MojoExecutionException {
		LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
		assertEquals(0, commandLine.getVMArguments().length);
	}

	public void testCreateCommandLineProgramArgs() throws MojoExecutionException {
		LaunchConfiguration commandLine = runMojo.createCommandLine(installation);
		List<String> programArgs = Arrays.asList(commandLine.getProgramArguments());
		assertTrue(programArgs.containsAll(List.of("-install", installation.getLocation().getAbsolutePath(), //
				"-configuration", new File(workFolder, "configuration").getAbsolutePath(), //
				"-data", new File(workFolder, "data").getAbsolutePath() //
		)));
	}

	public void testDataDirectoryIsClearedBeforeLaunching() throws IOException, MojoExecutionException {
		File markerFile = new File(workFolder, "data/markerfile").getAbsoluteFile();
		markerFile.getParentFile().mkdirs();
		markerFile.createNewFile();
		assertTrue(markerFile.exists());
		runMojo.runEclipse(installation);
		assertFalse(markerFile.exists());
	}

	public void testExecutionEnvironmentIsRespectedDuringDependencyResolution() throws Exception {
		AtomicReference<ExecutionEnvironmentConfiguration> recordedExecutionEnvironmentConfiguration = new AtomicReference<>();

		TargetPlatform mockTargetPlatform = mock(TargetPlatform.class);
		TargetPlatformFactory mockTargetPlatformFactory = mock(TargetPlatformFactory.class);
		when(mockTargetPlatformFactory.createTargetPlatform(any(), any(), any())).thenAnswer(invocation -> {
			recordedExecutionEnvironmentConfiguration.set(invocation.getArgument(1));
			return mockTargetPlatform;
		});

		P2Resolver mockP2Resolver = mock(P2Resolver.class);

		P2ResolverFactory mockP2ResolverFactory = mock(P2ResolverFactory.class);
		when(mockP2ResolverFactory.getTargetPlatformFactory()).thenReturn(mockTargetPlatformFactory);
		when(mockP2ResolverFactory.createResolver(any())).thenReturn(mockP2Resolver);

		EquinoxServiceFactory mockEquinoxServiceFactory = mock(EquinoxServiceFactory.class);
		when(mockEquinoxServiceFactory.getService(P2ResolverFactory.class)).thenReturn(mockP2ResolverFactory);
		setVariableValueToObject(runMojo, "equinox", mockEquinoxServiceFactory);

		setVariableValueToObject(runMojo, "executionEnvironment", "custom-ee");

		runMojo.execute();

		assertEquals("custom-ee", recordedExecutionEnvironmentConfiguration.get().getProfileName());
	}

	public void testExecutionEnvironmentIsRespectedDuringEclipseExecution() throws Exception {
		setVariableValueToObject(runMojo, "executionEnvironment", "custom-ee");
		@SuppressWarnings("deprecation")
		org.apache.maven.toolchain.java.DefaultJavaToolChain customToolchain = mock(
				org.apache.maven.toolchain.java.DefaultJavaToolChain.class);
		when(customToolchain.findTool("java")).thenReturn("/path/to/custom-ee-jdk/bin/java");
		when(toolchainProvider.findMatchingJavaToolChain(any(), any())).thenAnswer(invocation -> {
			return customToolchain;
		});

		LaunchConfiguration commandLine = runMojo.createCommandLine(installation);

		assertEquals("/path/to/custom-ee-jdk/bin/java", commandLine.getJvmExecutable());
	}

}
