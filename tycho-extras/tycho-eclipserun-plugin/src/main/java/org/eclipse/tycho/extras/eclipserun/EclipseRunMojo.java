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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.maven.ToolchainProvider;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.plugins.p2.extras.Repository;

/**
 * Launch an eclipse process with arbitrary commandline arguments. The eclipse
 * installation is defined by the dependencies to bundles specified.
 */
@Mojo(name = "eclipse-run", threadSafe = true)
public class EclipseRunMojo extends AbstractMojo {

	/**
	 * Lock object to ensure thread-safety
	 */
	private static final Object CREATE_LOCK = new Object();

	private static final ConcurrentMap<String, Object> WORKSPACE_LOCKS = new ConcurrentHashMap<>();

	/**
	 * Work area. This includes:
	 * <ul>
	 * <li><b>&lt;work&gt;/configuration</b>: The configuration area
	 * (<b>-configuration</b>)
	 * <li><b>&lt;work&gt;/data</b>: The data ('workspace') area (<b>-data</b>)
	 * </ul>
	 */
	@Parameter(defaultValue = "${project.build.directory}/eclipserun-work")
	private File work;

	/**
	 * Whether the workspace should be cleared before running eclipse.
	 * <p>
	 * If {@code false} and a workspace from a previous run exists, that workspace
	 * is reused.
	 * </p>
	 */
	@Parameter(defaultValue = "true")
	private boolean clearWorkspaceBeforeLaunch;

	@Parameter(property = "project")
	private MavenProject project;

	/**
	 * Dependencies which will be resolved transitively to make up the eclipse
	 * runtime. Example:
	 * 
	 * <pre>
	 * &lt;dependencies&gt;
	 *  &lt;dependency&gt;
	 *   &lt;artifactId&gt;org.eclipse.ant.core&lt;/artifactId&gt;
	 *   &lt;type&gt;eclipse-plugin&lt;/type&gt;
	 *  &lt;/dependency&gt;
	 * &lt;/dependencies&gt;
	 * </pre>
	 */
	@Parameter
	private List<Dependency> dependencies = new ArrayList<>();

	/**
	 * Whether to add default dependencies to bundles org.eclipse.equinox.launcher,
	 * org.eclipse.osgi and org.eclipse.core.runtime.
	 */
	@Parameter(defaultValue = "true")
	private boolean addDefaultDependencies;

	/**
	 * Execution environment profile name used to resolve dependencies.
	 */
	@Parameter(defaultValue = "JavaSE-11")
	private String executionEnvironment;

	/**
	 * p2 repositories which will be used to resolve dependencies. Example:
	 * 
	 * <pre>
	 * &lt;repositories&gt;
	 *  &lt;repository&gt;
	 *   &lt;id&gt;juno&lt;/id&gt;
	 *   &lt;layout&gt;p2&lt;/layout&gt;
	 *   &lt;url&gt;https://download.eclipse.org/releases/juno&lt;/url&gt;
	 *  &lt;/repository&gt;
	 * &lt;/repositories&gt;
	 * </pre>
	 */
	@Parameter(required = true)
	private List<Repository> repositories;

	@Parameter(property = "session", readonly = true, required = true)
	private MavenSession session;

	/**
	 * List of JVM arguments set on the command line. Example:
	 * 
	 * {@code
	 * <jvmArgs>
	 *   <arg>-Xdebug<arg>
	 *   <arg>-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044</arg>
	 * </jvmArgs>
	 * }
	 * 
	 * @since 0.25.0
	 */
	@Parameter
	private List<String> jvmArgs;

	/**
	 * Whether to skip mojo execution.
	 */
	@Parameter(property = "eclipserun.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * List of applications arguments set on the command line. Example:
	 * 
	 * {@code
	 * <applicationArgs>
	 *   <arg>-buildfile</arg>
	 *   <arg>build-test.xml</arg>
	 * </applicationArgs>
	 * }
	 * 
	 * @since 0.24.0
	 */
	@Parameter(alias = "applicationsArgs")
	private List<String> applicationArgs;

	/**
	 * Kill the forked process after a certain number of seconds. If set to 0, wait
	 * forever for the process, never timing out.
	 */
	@Parameter(property = "eclipserun.timeout")
	private int forkedProcessTimeoutInSeconds;

	/**
	 * Additional environments to set for the forked JVM.
	 */
	@Parameter
	private Map<String, String> environmentVariables;

	/**
	 * Bundle start level and auto start configuration used by the eclipse runtime.
	 * Example:
	 *
	 * <pre>
	 * &lt;bundleStartLevel&gt;
	 *   &lt;bundle&gt;
	 *     &lt;id&gt;foo.bar.myplugin&lt;/id&gt;
	 *     &lt;level&gt;6&lt;/level&gt;
	 *     &lt;autoStart&gt;true&lt;/autoStart&gt;
	 *   &lt;/bundle&gt;
	 * &lt;/bundleStartLevel&gt;
	 * </pre>
	 */
	@Parameter
	private BundleStartLevel[] bundleStartLevel;

	/**
	 * The default bundle start level and auto start configuration used by the
	 * runtime for bundles where the start level/auto start is not configured in
	 * {@link #bundleStartLevel}. Example:
	 *
	 * <pre>
	 *   &lt;defaultStartLevel&gt;
	 *     &lt;level&gt;6&lt;/level&gt;
	 *     &lt;autoStart&gt;true&lt;/autoStart&gt;
	 *   &lt;/defaultStartLevel&gt;
	 * </pre>
	 */
	@Parameter
	private BundleStartLevel defaultStartLevel;

	@Component
	private EquinoxInstallationFactory installationFactory;

	@Component
	private EquinoxLauncher launcher;

	@Component
	private ToolchainProvider toolchainProvider;

	@Component
	private EquinoxServiceFactory equinox;

	@Component
	private Logger logger;

	@Component
	private ToolchainManager toolchainManager;

	public EclipseRunMojo() {
		// default constructor
	}

	/**
	 * Constructor for use of EclipseRunMojo in other Mojos.
	 */
	public EclipseRunMojo(File work, boolean clearWorkspaceBeforeLaunch, MavenProject project,
			List<Dependency> dependencies, boolean addDefaultDependencies, String executionEnvironment,
			List<Repository> repositories, MavenSession session, List<String> jvmArgs, boolean skip,
			List<String> applicationArgs, int forkedProcessTimeoutInSeconds, Map<String, String> environmentVariables,
			EquinoxInstallationFactory installationFactory, EquinoxLauncher launcher,
			ToolchainProvider toolchainProvider, EquinoxServiceFactory equinox, Logger logger,
			ToolchainManager toolchainManager) {
		this.work = work;
		this.clearWorkspaceBeforeLaunch = clearWorkspaceBeforeLaunch;
		this.project = project;
		this.dependencies = dependencies;
		this.addDefaultDependencies = addDefaultDependencies;
		this.executionEnvironment = executionEnvironment;
		this.repositories = repositories;
		this.session = session;
		this.jvmArgs = jvmArgs;
		this.skip = skip;
		this.applicationArgs = applicationArgs;
		this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
		this.environmentVariables = environmentVariables;
		this.installationFactory = installationFactory;
		this.launcher = launcher;
		this.toolchainProvider = toolchainProvider;
		this.equinox = equinox;
		this.logger = logger;
		this.toolchainManager = toolchainManager;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().debug("skipping mojo execution");
			return;
		}
		EquinoxInstallation installation;
		synchronized (CREATE_LOCK) {
			installation = createEclipseInstallation();
		}
		runEclipse(installation);
	}

	private void addDefaultDependency(P2Resolver resolver, String bundleId) {
		try {
			resolver.addDependency(ArtifactType.TYPE_ECLIPSE_PLUGIN, bundleId, null);
		} catch (IllegalArtifactReferenceException e) {
			// shouldn't happen for the constant type and version
			throw new RuntimeException(e);
		}
	}

	private void addDefaultDependencies(P2Resolver resolver) {
		if (addDefaultDependencies) {
			addDefaultDependency(resolver, "org.eclipse.osgi");
			addDefaultDependency(resolver, EquinoxInstallationDescription.EQUINOX_LAUNCHER);
			addDefaultDependency(resolver, "org.eclipse.core.runtime");
		}
	}

	private EquinoxInstallation createEclipseInstallation() throws MojoFailureException {
		P2ResolverFactory resolverFactory = equinox.getService(P2ResolverFactory.class);
		TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
		// we want to resolve from remote repos only
		tpConfiguration.setForceIgnoreLocalArtifacts(true);
		for (Repository repository : repositories) {
			tpConfiguration.addP2Repository(new MavenRepositoryLocation(repository.getId(), repository.getLocation()));
		}
		ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger, false,
				toolchainManager, session);
		TargetPlatform targetPlatform = resolverFactory.getTargetPlatformFactory().createTargetPlatform(tpConfiguration,
				eeConfiguration, null);
		P2Resolver resolver = resolverFactory.createResolver(new MavenLoggerAdapter(logger, false));
		for (Dependency dependency : dependencies) {
			try {
				resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
			} catch (IllegalArtifactReferenceException e) {
				throw new MojoFailureException("Invalid dependency " + dependency.getType() + ":"
						+ dependency.getArtifactId() + ":" + dependency.getVersion() + ": " + e.getMessage(), e);
			}
		}
		addDefaultDependencies(resolver);
		EquinoxInstallationDescription installationDesc = new DefaultEquinoxInstallationDescription();
		for (P2ResolutionResult result : resolver.resolveTargetDependencies(targetPlatform, null).values()) {
			for (Entry entry : result.getArtifacts()) {
				if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(entry.getType())) {
					installationDesc.addBundle(
							new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, entry.getId(), entry.getVersion()),
							entry.getLocation(true));
				}
			}
		}
		installationDesc.setDefaultBundleStartLevel(defaultStartLevel);
		if (bundleStartLevel != null) {
			for (BundleStartLevel level : bundleStartLevel) {
				installationDesc.addBundleStartLevel(level);
			}
		}
		return installationFactory.createInstallation(installationDesc, work);
	}

	void runEclipse(EquinoxInstallation runtime) throws MojoExecutionException {
		try {
			File workspace = new File(work, "data").getAbsoluteFile();
			synchronized (WORKSPACE_LOCKS.computeIfAbsent(workspace.getAbsolutePath(), k -> new Object())) {
				if (clearWorkspaceBeforeLaunch) {
					FileUtils.deleteDirectory(workspace);
				}
				LaunchConfiguration cli = createCommandLine(runtime);
				File expectedLog = new File(workspace, ".metadata/.log");
				getLog().info("Expected eclipse log file: " + expectedLog.getCanonicalPath());
				int returnCode = launcher.execute(cli, forkedProcessTimeoutInSeconds);
				if (returnCode != 0) {
					throw new MojoExecutionException("Error while executing platform: return code=" + returnCode
							+ ", see content of " + expectedLog + "for more details.");
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Error while executing platform", e);
		}
	}

	LaunchConfiguration createCommandLine(EquinoxInstallation runtime) throws MojoExecutionException {
		EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(runtime);

		String executable = null;
		Toolchain tc = getToolchain();
		if (tc != null) {
			getLog().info("Toolchain in tycho-eclipserun-plugin: " + tc);
			executable = tc.findTool("java");
			if (executable == null) {
				getLog().error("No 'java' executable was found in toolchain. Current java runtime will be used.");
			}
		} else if (Objects.equals(executionEnvironment, "JavaSE-" + Runtime.version().feature())) {
			getLog().debug("Using current java runtime as it matches configured executionEnvironment.");
		} else {
			getLog().warn("No toolchain was found in tycho-eclipserun-plugin for: " + executionEnvironment
					+ " .Current java runtime will be used.");
		}
		cli.setJvmExecutable(executable);
		cli.setWorkingDirectory(project.getBasedir());

		if (jvmArgs != null) {
			cli.addVMArguments(jvmArgs.toArray(new String[jvmArgs.size()]));
		}

		addProgramArgs(cli, "-install", runtime.getLocation().getAbsolutePath(), "-configuration",
				new File(work, "configuration").getAbsolutePath());

		File workspace = new File(work, "data");
		addProgramArgs(cli, "-data", workspace.getAbsolutePath());

		if (applicationArgs != null) {
			for (String arg : applicationArgs) {
				cli.addProgramArguments(arg);
			}
		}

		if (environmentVariables != null) {
			cli.addEnvironmentVariables(environmentVariables);
		}

		return cli;
	}

	private void addProgramArgs(EquinoxLaunchConfiguration cli, String... arguments) {
		if (arguments != null) {
			for (String argument : arguments) {
				if (argument != null) {
					cli.addProgramArguments(argument);
				}
			}
		}
	}

	private Toolchain getToolchain() throws MojoExecutionException {
		return toolchainProvider.findMatchingJavaToolChain(session, executionEnvironment);
	}

}
