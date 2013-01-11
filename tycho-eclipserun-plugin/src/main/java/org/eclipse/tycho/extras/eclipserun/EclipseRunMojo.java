/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Marc-Andre Laperle - EclipseRunMojo inspired by TestMojo
 *******************************************************************************/
package org.eclipse.tycho.extras.eclipserun;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;
import org.eclipse.tycho.plugins.p2.extras.Repository;

/**
 * Launch an eclipse process with arbitrary commandline arguments. The eclipse installation is
 * defined by the dependencies to bundles specified.
 * 
 * @goal eclipse-run
 */
public class EclipseRunMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project.build.directory}/work"
     */
    private File work;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * Dependencies which will be resolved transitively to make up the eclipse runtime. Example:
     * 
     * <pre>
     * &lt;dependencies&gt;
     *  &lt;dependency&gt;
     *   &lt;artifactId&gt;org.eclipse.ant.core&lt;/artifactId&gt;
     *   &lt;type&gt;eclipse-plugin&lt;/type&gt;
     *  &lt;/dependency&gt;
     * &lt;/dependencies&gt;
     * </pre>
     * 
     * @parameter
     */
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    /**
     * Whether to add default dependencies to bundles org.eclipse.equinox.launcher, org.eclipse.osgi
     * and org.eclipse.core.runtime.
     * 
     * @parameter default-value="true"
     */
    private boolean addDefaultDependencies;

    /**
     * Execution environment profile name used to resolve dependencies.
     * 
     * @parameter default-value="JavaSE-1.6"
     */
    private String executionEnvironment;

    /**
     * p2 repositories which will be used to resolve dependencies. Example:
     * 
     * <pre>
     * &lt;repositories&gt;
     *  &lt;repository&gt;
     *   &lt;id&gt;juno&lt;/id&gt;
     *   &lt;layout&gt;p2&lt;/layout&gt;
     *   &lt;url&gt;http://download.eclipse.org/releases/juno&lt;/url&gt;
     *  &lt;/repository&gt;
     * &lt;/repositories&gt;
     * </pre>
     * 
     * @parameter
     * @required
     */
    private List<Repository> repositories;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter
     */
    private String argLine;

    /**
     * Whether to skip mojo execution.
     * 
     * @parameter expression="${eclipserun.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Arbitrary applications arguments to set on the command line.
     * 
     * @parameter
     */
    private String appArgLine;

    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the
     * process, never timing out.
     * 
     * @parameter expression="${eclipserun.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Additional environments to set for the forked JVM.
     * 
     * @parameter
     */
    private Map<String, String> environmentVariables;

    /** @component */
    private EquinoxInstallationFactory installationFactory;

    /** @component */
    private EquinoxLauncher launcher;

    /** @component */
    private ToolchainManager toolchainManager;

    /** @component */
    private EquinoxServiceFactory equinox;

    /** @component */
    private Logger logger;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().debug("skipping mojo execution");
            return;
        }
        EquinoxInstallation installation = createEclipseInstallation();
        runEclipse(installation);
    }

    private Dependency newBundleDependency(String bundleId) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId(bundleId);
        dependency.setType(ArtifactKey.TYPE_ECLIPSE_PLUGIN);
        return dependency;
    }

    private List<Dependency> getDefaultDependencies() {
        ArrayList<Dependency> result = new ArrayList<Dependency>();
        result.add(newBundleDependency("org.eclipse.osgi"));
        result.add(newBundleDependency(EquinoxInstallationDescription.EQUINOX_LAUNCHER));
        result.add(newBundleDependency("org.eclipse.core.runtime"));
        return result;
    }

    private EquinoxInstallation createEclipseInstallation() throws MojoExecutionException {
        P2ResolverFactory resolverFactory = equinox.getService(P2ResolverFactory.class);
        TargetPlatformBuilder tpBuilder = resolverFactory
                .createTargetPlatformBuilder(new ExecutionEnvironmentConfigurationStub(executionEnvironment));
        // we want to resolve from remote repos only
        tpBuilder.setIncludeLocalMavenRepo(false);
        for (Repository repository : repositories) {
            tpBuilder.addP2Repository(new MavenRepositoryLocation(repository.getId(), repository.getLocation()));
        }
        TargetPlatform targetPlatform = tpBuilder.buildTargetPlatform();
        P2Resolver resolver = resolverFactory.createResolver(new MavenLoggerAdapter(logger, false));
        for (Dependency dependency : dependencies) {
            resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
        }
        if (addDefaultDependencies) {
            for (Dependency dependency : getDefaultDependencies()) {
                resolver.addDependency(dependency.getType(), dependency.getArtifactId(), dependency.getVersion());
            }
        }
        EquinoxInstallationDescription installationDesc = new DefaultEquinoxInstallationDescription();
        for (P2ResolutionResult result : resolver.resolveDependencies(targetPlatform, null)) {
            for (Entry entry : result.getArtifacts()) {
                installationDesc.addBundle(
                        new DefaultArtifactKey(ArtifactKey.TYPE_ECLIPSE_PLUGIN, entry.getId(), entry.getVersion()),
                        entry.getLocation());
            }
        }
        return installationFactory.createInstallation(installationDesc, work);
    }

    private void runEclipse(EquinoxInstallation runtime) throws MojoExecutionException, MojoFailureException {
        try {
            File workspace = new File(work, "data").getAbsoluteFile();
            FileUtils.deleteDirectory(workspace);
            LaunchConfiguration cli = createCommandLine(runtime);
            getLog().info("Expected eclipse log file: " + new File(workspace, ".metadata/.log").getCanonicalPath());
            int returnCode = launcher.execute(cli, forkedProcessTimeoutInSeconds);
            if (returnCode != 0) {
                throw new MojoExecutionException("Error while executing platform (return code: " + returnCode + ")");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while executing platform", e);
        }
    }

    LaunchConfiguration createCommandLine(EquinoxInstallation runtime) throws MalformedURLException {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(runtime);

        String executable = null;
        Toolchain tc = getToolchain();
        if (tc != null) {
            getLog().info("Toolchain in tycho-eclipserun-plugin: " + tc);
            executable = tc.findTool("java");
        }
        cli.setJvmExecutable(executable);
        cli.setWorkingDirectory(project.getBasedir());

        if (argLine != null) {
            cli.addVMArguments(false, argLine);
        }

        addProgramArgs(true, cli, "-install", runtime.getLocation().getAbsolutePath(), "-configuration", new File(work,
                "configuration").getAbsolutePath());

        addProgramArgs(false, cli, appArgLine);

        if (environmentVariables != null) {
            cli.addEnvironmentVariables(environmentVariables);
        }

        return cli;
    }

    private void addProgramArgs(boolean escape, EquinoxLaunchConfiguration cli, String... arguments) {
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument != null) {
                    cli.addProgramArguments(escape, argument);
                }
            }
        }
    }

    private Toolchain getToolchain() {
        Toolchain tc = null;
        if (toolchainManager != null) {
            tc = toolchainManager.getToolchainFromBuildContext("jdk", session);
        }
        return tc;
    }

}
