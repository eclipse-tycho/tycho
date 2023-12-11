/*******************************************************************************
 * Copyright (c) 2011, 2016 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Marc-Andre Laperle - EclipseRunMojo inspired by TestMojo
 *******************************************************************************/
package org.eclipse.tycho.extras.eclipserun;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.eclipserun.Repository;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;

/**
 * Launch an eclipse process with arbitrary commandline arguments. The eclipse
 * installation is defined by the dependencies to bundles specified.
 */
@Mojo(name = "eclipse-run", threadSafe = true)
@Deprecated
public class EclipseRunMojo extends org.eclipse.tycho.eclipserun.EclipseRunMojo {

	public EclipseRunMojo() {
		// default constructor
	}

	/**
	 * Constructor for use of EclipseRunMojo in other Mojos.
	 * 
	 * @param platformFactory
	 */
	public EclipseRunMojo(File work, boolean clearWorkspaceBeforeLaunch, MavenProject project,
			List<Dependency> dependencies, boolean addDefaultDependencies, String executionEnvironment,
			List<Repository> repositories, MavenSession session, List<String> jvmArgs, boolean skip,
			List<String> applicationArgs, int forkedProcessTimeoutInSeconds, Map<String, String> environmentVariables,
			EquinoxInstallationFactory installationFactory, EquinoxLauncher launcher,
			ToolchainProvider toolchainProvider, P2ResolverFactory resolverFactory, Logger logger,
			ToolchainManager toolchainManager, TargetPlatformFactory platformFactory) {
		super(work, clearWorkspaceBeforeLaunch, project, dependencies, addDefaultDependencies, executionEnvironment,
				repositories, session, jvmArgs, skip, applicationArgs, forkedProcessTimeoutInSeconds,
				environmentVariables, installationFactory, launcher, toolchainProvider, resolverFactory, logger,
				toolchainManager, platformFactory);
	}
}
