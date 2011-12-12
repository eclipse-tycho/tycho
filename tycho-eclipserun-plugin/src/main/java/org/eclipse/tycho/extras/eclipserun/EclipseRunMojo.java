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
import java.util.Arrays;
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
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.launching.DefaultEquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationFactory;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformResolverFactory;
import org.eclipse.tycho.launching.LaunchConfiguration;
/**
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
     * Additional target platform dependencies.
     * 
     * @parameter
     */
    private Dependency[] dependencies;
    
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
     * Arbitrary applications arguments to set on the command line.
     * 
     * @parameter
     */
    private String appArgLine;
    
    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     * 
     * @parameter expression="${surefire.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;
    
    /**
     * Additional environments to set for the forked JVM.
     * 
     * @parameter
     */
    private Map<String, String> environmentVariables;
      
    /** @component */
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;
    
    /** @component */
    private EquinoxInstallationFactory installationFactory;
    
    /** @component */
    private EquinoxLauncher launcher;
    
    /** @component */
    private ToolchainManager toolchainManager;
    
    public void execute() throws MojoExecutionException, MojoFailureException {

        EquinoxInstallation installation = createEclipseInstallation(false, DefaultReactorProject.adapt(session));
        runEclipse(installation);
    }
    
    private Dependency newBundleDependency(String bundleId) {
        Dependency ideapp = new Dependency();
        ideapp.setArtifactId(bundleId);
        ideapp.setType(ArtifactKey.TYPE_ECLIPSE_PLUGIN);
        return ideapp;
    }
    
    private List<Dependency> getBasicDependencies() {
        ArrayList<Dependency> result = new ArrayList<Dependency>();

        result.add(newBundleDependency("org.eclipse.osgi"));
        result.add(newBundleDependency(EquinoxInstallationDescription.EQUINOX_LAUNCHER));
        result.add(newBundleDependency("org.eclipse.core.runtime"));

        return result;
    }
    
    private EquinoxInstallation createEclipseInstallation(boolean includeReactorProjects,
            List<ReactorProject> reactorProjects) throws MojoExecutionException {
        TargetPlatformResolver platformResolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

        List<Dependency> dependencies = getBasicDependencies();
        if (this.dependencies != null) {
            dependencies.addAll(Arrays.asList(this.dependencies));
        }
        
        DependencyArtifacts runtimeArtifacts = platformResolver.resolvePlatform(session, project, reactorProjects,
                dependencies);

        EquinoxInstallationDescription installationDesc = new DefaultEquinoxInstallationDescription();

        for (ArtifactDescriptor artifact : runtimeArtifacts.getArtifacts(ArtifactKey.TYPE_ECLIPSE_PLUGIN)) {
            installationDesc.addBundle(artifact);
        }

        return installationFactory.createInstallation(installationDesc, work);
    }
    
    private void runEclipse(EquinoxInstallation runtime) throws MojoExecutionException, MojoFailureException {
        try {
            File workspace = new File(work, "data").getAbsoluteFile();
            FileUtils.deleteDirectory(workspace);
            LaunchConfiguration cli = createCommandLine(runtime);
            getLog().info("Expected eclipse log file: " + new File(workspace, ".metadata/.log").getCanonicalPath());
            launcher.execute(cli, forkedProcessTimeoutInSeconds);
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
        
        addProgramArgs(true, cli,
                "-install", runtime.getLocation().getAbsolutePath(),
                "-configuration", new File(work, "configuration").getAbsolutePath());
        
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
