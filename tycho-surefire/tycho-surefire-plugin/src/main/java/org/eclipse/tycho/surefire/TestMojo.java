/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG        - port to surefire 2.10
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
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
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.SimpleDependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformResolverFactory;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.launching.LaunchConfiguration;
import org.eclipse.tycho.launching.LaunchConfigurationFactory;
import org.osgi.framework.Version;

/**
 * @phase integration-test
 * @goal test
 * @requiresProject true
 * @requiresDependencyResolution runtime
 */
public class TestMojo extends AbstractMojo implements LaunchConfigurationFactory {

    /**
     * @parameter default-value="${project.build.directory}/work"
     */
    private File work;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${debugPort}"
     */
    private int debugPort;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in
     * testing. When not specified and whent the <code>test</code> parameter is not specified, the
     * default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
     * 
     * @parameter
     */
    private List<String> includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in
     * testing. When not specified and whent the <code>test</code> parameter is not specified, the
     * default excludes will be
     * <code>**&#47;Abstract*Test.java  **&#47;Abstract*TestCase.java **&#47;*$*</code>
     * 
     * @parameter
     */
    private List<String> excludes;

    /**
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern
     * matching, to select tests to run. The Ant pattern will be used to create an include pattern
     * formatted like <code>**&#47;${test}.java</code> When used, the <code>includes</code> and
     * <code>excludes</code> patterns parameters are ignored
     * 
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * @parameter expression="${maven.test.skipExec}" default-value="false"
     * @deprecated Use skipTests instead.
     */
    private boolean skipExec;

    /**
     * @parameter expression="${skipTests}" default-value="false"
     * 
     *            Set this to "true" to skip running tests, but still compile them. Its use is NOT
     *            RECOMMENDED, but quite convenient on occasion.
     */
    private boolean skipTests;

    /**
     * @parameter expression="${maven.test.skip}" default-value="false"
     * 
     *            Same as {@link #skipTests}
     */
    private boolean skip;

    /**
     * If set to "false" the test execution will not fail in case there are no tests found.
     * 
     * @parameter expression="${failIfNoTests}" default-value="true"
     */
    private boolean failIfNoTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     * 
     * @parameter expression="${maven.test.failure.ignore}" default-value="false"
     */
    private boolean testFailureIgnore;

    /**
     * The directory containing generated test classes of the project being tested.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * Enables -debug -consolelog for the test OSGi runtime
     * 
     * @parameter expression="${tycho.showEclipseLog}" default-value="false"
     */
    private boolean showEclipseLog;

    /**
     * Set this to "true" to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     * 
     * @parameter expression="${maven.test.redirectTestOutputToFile}" default-value="false"
     */
    private boolean redirectTestOutputToFile;

    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;

    /** @parameter expression="${project.build.directory}/surefire.properties" */
    private File surefireProperties;

    /** @parameter expression="${project.build.directory}/dev.properties" */
    private File devProperties;

    /**
     * Additional test target platform dependencies.
     * 
     * @parameter
     */
    private Dependency[] dependencies;

    /**
     * Eclipse application to be run. If not specified, default application
     * org.eclipse.ui.ide.workbench will be used. Application runnable will be invoked from test
     * harness, not directly from Eclipse.
     * 
     * @parameter
     */
    private String application;

    /**
     * Eclipse product to be run, i.e. -product parameter passed to test Eclipse runtime.
     * 
     * @parameter
     */
    private String product;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * Run tests using UI (true) or headless (false) test harness.
     * 
     * @parameter default-value="false"
     */
    private boolean useUIHarness;

    /**
     * Run tests in UI (true) or background (false) thread. Only applies to UI test harness.
     * 
     * @parameter default-value="true"
     */
    private boolean useUIThread;

    /**
     * @parameter expression="${plugin.artifacts}"
     */
    private List<Artifact> pluginArtifacts;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter expression="${tycho.testArgLine}"
     */
    private String argLine;

    /**
     * Arbitrary applications arguments to set on the command line.
     * 
     * @parameter
     */
    private String appArgLine;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for
     * the process, never timing out.
     * 
     * @parameter expression="${surefire.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Bundle-SymbolicName of the test suite, a special bundle that knows how to locate and execute
     * all relevant tests.
     * 
     * testSuite and testClass identify single test class to run. All other tests will be ignored if
     * both testSuite and testClass are provided. It is an error if provide one of the two
     * parameters but not the other.
     * 
     * @parameter expression="${testSuite}"
     */
    private String testSuite;

    /**
     * See testSuite
     * 
     * @parameter expression="${testClass}"
     */
    private String testClass;

    /**
     * Additional environments to set for the forked test JVM.
     * 
     * @parameter
     */
    private Map<String, String> environmentVariables;

    /**
     * Additional system properties to set for the forked test JVM.
     * 
     * @parameter
     */
    private Map<String, String> systemProperties;

    /**
     * List of bundles that must be expanded in order to execute the tests
     * 
     * @parameter
     */
    private String[] explodedBundles;

    /**
     * List of framework extension bundles to add.
     * 
     * @parameter
     */
    private Dependency[] frameworkExtensions;

    /**
     * Bundle start level and auto start configuration used by the test runtime.
     * 
     * @parameter
     */
    private BundleStartLevel[] bundleStartLevel;

    /**
     * @component
     */
    private RepositorySystem repositorySystem;

    /**
     * @component
     */
    private ResolutionErrorHandler resolutionErrorHandler;

    /** @component */
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /** @component */
    private EquinoxInstallationFactory installationFactory;

    /** @component */
    private EquinoxLauncher launcher;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject" role-hint="eclipse-plugin"
     */
    private OsgiBundleProject osgiBundle;

    /** @component */
    private ToolchainManager toolchainManager;

    /** @component */
    private BuildPropertiesParser buildPropertiesParser;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipExec || skipTests) {
            getLog().info("Skipping tests");
            return;
        }

        if (!"eclipse-test-plugin".equals(project.getPackaging())) {
            getLog().warn("Unsupported packaging type " + project.getPackaging());
            return;
        }

        if (testSuite != null || testClass != null) {
            if (testSuite == null || testClass == null) {
                throw new MojoExecutionException("Both testSuite and testClass must be provided or both should be null");
            }

            MavenProject suite = getTestSuite(testSuite);

            if (suite == null) {
                throw new MojoExecutionException("Cannot find test suite project with Bundle-SymbolicName " + testSuite);
            }

            if (!suite.equals(project)) {
                getLog().info("Not executing tests, testSuite=" + testSuite + " and project is not the testSuite");
                return;
            }
        }

        EquinoxInstallation testRuntime = createEclipseInstallation(false, DefaultReactorProject.adapt(session));
        runTest(testRuntime);
    }

    private EquinoxInstallation createEclipseInstallation(boolean includeReactorProjects,
            List<ReactorProject> reactorProjects) throws MojoExecutionException {
        TargetPlatformResolver platformResolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

        ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

        if (this.dependencies != null) {
            dependencies.addAll(Arrays.asList(this.dependencies));
        }

        dependencies.addAll(getTestDependencies());

        // TODO 364134 re-use target platform from dependency resolution
        TargetPlatform targetPlatform = platformResolver.computeTargetPlatform(session, project, reactorProjects);

        DependencyArtifacts testRuntimeArtifacts = platformResolver.resolveDependencies(session, project,
                targetPlatform, reactorProjects, new SimpleDependencyResolverConfiguration(dependencies));

        if (testRuntimeArtifacts == null) {
            throw new MojoExecutionException("Cannot determinate build target platform location -- not executing tests");
        }

        work.mkdirs();

        EquinoxInstallationDescription testRuntime = new DefaultEquinoxInstallationDescription();
        testRuntime.addBundlesToExplode(getBundlesToExplode());
        testRuntime.addFrameworkExtensions(getFrameworkExtensions());
        if (bundleStartLevel != null) {
            for (BundleStartLevel level : bundleStartLevel) {
                testRuntime.addBundleStartLevel(level);
            }
        }

        BundleProject projectType = (BundleProject) projectTypes.get(project.getPackaging());
        String testFramework = new TestFramework().getTestFramework(projectType.getClasspath(project));
        if (testFramework == null) {
            throw new MojoExecutionException("Could not determine test framework used by test bundle "
                    + project.toString());
        }
        getLog().debug("Using test framework " + testFramework);

        for (ArtifactDescriptor artifact : testRuntimeArtifacts.getArtifacts(ArtifactKey.TYPE_ECLIPSE_PLUGIN)) {
            // note that this project is added as directory structure rooted at project basedir.
            // project classes and test-classes are added via dev.properties file (see #createDevProperties())
            // all other projects are added as bundle jars.
            ReactorProject otherProject = artifact.getMavenProject();
            if (otherProject != null) {
                if (otherProject.sameProject(project)) {
                    testRuntime.addBundle(artifact.getKey(), project.getBasedir());
                    continue;
                }
                File file = otherProject.getArtifact(artifact.getClassifier());
                if (file != null) {
                    testRuntime.addBundle(artifact.getKey(), file);
                    continue;
                }
            }
            testRuntime.addBundle(artifact);
        }

        Set<File> surefireBundles = getSurefirePlugins(testFramework);
        for (File file : surefireBundles) {
            testRuntime.addBundle(getBundleArtifacyKey(file), file, true);
        }

        createDevProperties(includeReactorProjects, reactorProjects);
        createSurefireProperties(projectType.getArtifactKey(DefaultReactorProject.adapt(project)).getId(),
                testFramework);

        reportsDirectory.mkdirs();
        return installationFactory.createInstallation(testRuntime, work);
    }

    private ArtifactKey getBundleArtifacyKey(File file) throws MojoExecutionException {
        ArtifactKey key = osgiBundle.readArtifactKey(file);
        if (key == null) {
            throw new MojoExecutionException("Not an OSGi bundle " + file.getAbsolutePath());
        }
        return key;
    }

    private MavenProject getTestSuite(String symbolicName) {
        for (MavenProject otherProject : session.getProjects()) {
            TychoProject projectType = projectTypes.get(otherProject.getPackaging());
            if (projectType != null
                    && projectType.getArtifactKey(DefaultReactorProject.adapt(otherProject)).getId()
                            .equals(symbolicName)) {
                return otherProject;
            }
        }
        return null;
    }

    private List<Dependency> getTestDependencies() {
        ArrayList<Dependency> result = new ArrayList<Dependency>();

        result.add(newBundleDependency("org.eclipse.osgi"));
        result.add(newBundleDependency(EquinoxInstallationDescription.EQUINOX_LAUNCHER));
        if (useUIHarness) {
            result.add(newBundleDependency("org.eclipse.ui.ide.application"));
        } else {
            result.add(newBundleDependency("org.eclipse.core.runtime"));
        }

        return result;
    }

    protected Dependency newBundleDependency(String bundleId) {
        Dependency ideapp = new Dependency();
        ideapp.setArtifactId(bundleId);
        ideapp.setType(ArtifactKey.TYPE_ECLIPSE_PLUGIN);
        return ideapp;
    }

    private void createSurefireProperties(String symbolicName, String testFramework) throws MojoExecutionException {
        Properties p = new Properties();

        p.put("testpluginname", symbolicName);
        p.put("testclassesdirectory", testClassesDirectory.getAbsolutePath());
        p.put("reportsdirectory", reportsDirectory.getAbsolutePath());
        p.put("testprovider", getTestProvider(testFramework));
        p.put("redirectTestOutputToFile", String.valueOf(redirectTestOutputToFile));

        if (test != null) {
            String test = this.test;
            test = test.replace('.', '/');
            test = test.endsWith(".class") ? test : test + ".class";
            test = test.startsWith("**/") ? test : "**/" + test;
            p.put("includes", test);
        } else {
            if (testClass != null) {
                p.put("includes", testClass.replace('.', '/') + ".class");
            } else {
                p.put("includes", includes != null ? getIncludesExcludes(includes)
                        : "**/Test*.class,**/*Test.class,**/*TestCase.class");
                p.put("excludes", excludes != null ? getIncludesExcludes(excludes)
                        : "**/Abstract*Test.class,**/Abstract*TestCase.class,**/*$*");
            }
        }
        p.put("failifnotests", String.valueOf(failIfNoTests));
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(surefireProperties));
            try {
                p.store(out, null);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write test launcher properties file", e);
        }
    }

    private String getTestProvider(String testFramework) {
        if (TestFramework.TEST_JUNIT.equals(testFramework)) {
            return "org.apache.maven.surefire.junit.JUnit3Provider";
        } else if (TestFramework.TEST_JUNIT4.equals(testFramework)) {
            return "org.apache.maven.surefire.junit4.JUnit4Provider";
        }
        throw new IllegalArgumentException(); // can't happen
    }

    private String getIncludesExcludes(List<String> patterns) {
        StringBuilder sb = new StringBuilder();
        for (String pattern : patterns) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pattern);
        }
        return sb.toString();
    }

    private void runTest(EquinoxInstallation testRuntime) throws MojoExecutionException, MojoFailureException {
        int result;
        try {
            File workspace = new File(work, "data").getAbsoluteFile();
            FileUtils.deleteDirectory(workspace);
            LaunchConfiguration cli = createCommandLine(testRuntime, workspace);
            getLog().info("Expected eclipse log file: " + new File(workspace, ".metadata/.log").getCanonicalPath());
            result = launcher.execute(cli, forkedProcessTimeoutInSeconds);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while executing platform", e);
        }
        switch (result) {
        case 0:
            getLog().info("All tests passed!");
            break;
        case RunResult.NO_TESTS:
            String message = "No tests found.";
            if (failIfNoTests) {
                throw new MojoFailureException(message);
            } else {
                getLog().warn(message);
            }
            break;
        default:
            String errorMessage = "There are test failures.\n\nPlease refer to " + reportsDirectory
                    + " for the individual test results.";
            if (testFailureIgnore) {
                getLog().error(errorMessage);
            } else {
                throw new MojoFailureException(errorMessage);
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

    LaunchConfiguration createCommandLine(EquinoxInstallation testRuntime, File workspace) throws MalformedURLException {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(testRuntime);

        String executable = null;
        Toolchain tc = getToolchain();
        if (tc != null) {
            getLog().info("Toolchain in tycho-surefire-plugin: " + tc);
            executable = tc.findTool("java");
        }
        cli.setJvmExecutable(executable);

        cli.setWorkingDirectory(project.getBasedir());

        if (debugPort > 0) {
            cli.addVMArguments("-Xdebug", "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y");
        }

        cli.addVMArguments("-Dosgi.noShutdown=false");

        Properties properties = (Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
        cli.addVMArguments("-Dosgi.os=" + PlatformPropertiesUtils.getOS(properties), //
                "-Dosgi.ws=" + PlatformPropertiesUtils.getWS(properties), //
                "-Dosgi.arch=" + PlatformPropertiesUtils.getArch(properties));

        addVMArgs(cli, argLine);

        if (systemProperties != null) {
            for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
                cli.addVMArguments(true, "-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        if (getLog().isDebugEnabled() || showEclipseLog) {
            cli.addProgramArguments("-debug", "-consolelog");
        }

        addProgramArgs(true, cli, "-data", workspace.getAbsolutePath(), //
                "-dev", devProperties.toURI().toURL().toExternalForm(), //
                "-install", testRuntime.getLocation().getAbsolutePath(), //
                "-configuration", new File(work, "configuration").getAbsolutePath(), //
                "-application", getTestApplication(testRuntime.getInstallationDescription()), //
                "-testproperties", surefireProperties.getAbsolutePath());
        if (application != null) {
            cli.addProgramArguments("-testApplication", application);
        }
        if (product != null) {
            cli.addProgramArguments("-product", product);
        }
        if (useUIHarness && !useUIThread) {
            cli.addProgramArguments("-nouithread");
        }
        addProgramArgs(false, cli, appArgLine);
        if (environmentVariables != null) {
            cli.addEnvironmentVariables(environmentVariables);
        }
        return cli;
    }

    void addProgramArgs(boolean escape, EquinoxLaunchConfiguration cli, String... arguments) {
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument != null) {
                    cli.addProgramArguments(escape, argument);
                }
            }
        }
    }

    void addVMArgs(EquinoxLaunchConfiguration cli, String argLine) {
        if (argLine != null) {
            cli.addVMArguments(false, argLine);
        }
    }

    private String getTestApplication(EquinoxInstallationDescription testRuntime) {
        if (useUIHarness) {
            ArtifactDescriptor systemBundle = testRuntime.getSystemBundle();
            Version osgiVersion = Version.parseVersion(systemBundle.getKey().getVersion());
            if (osgiVersion.compareTo(EquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
                return "org.eclipse.tycho.surefire.osgibooter.uitest32";
            } else {
                return "org.eclipse.tycho.surefire.osgibooter.uitest";
            }
        } else {
            return "org.eclipse.tycho.surefire.osgibooter.headlesstest";
        }
    }

    private Set<File> getSurefirePlugins(String testFramework) throws MojoExecutionException {
        Set<File> result = new LinkedHashSet<File>();

        String fragment;
        if (TestFramework.TEST_JUNIT.equals(testFramework)) {
            fragment = "org.eclipse.tycho.surefire.junit";
        } else if (TestFramework.TEST_JUNIT4.equals(testFramework)) {
            fragment = "org.eclipse.tycho.surefire.junit4";
        } else {
            throw new IllegalArgumentException("Unsupported test framework " + testFramework);
        }

        for (Artifact artifact : pluginArtifacts) {
            if ("org.eclipse.tycho".equals(artifact.getGroupId())) {
                if ("org.eclipse.tycho.surefire.osgibooter".equals(artifact.getArtifactId())
                        || fragment.equals(artifact.getArtifactId())) {
                    result.add(artifact.getFile());
                }
            }
        }

        if (result.size() != 2) {
            StringBuilder sb = new StringBuilder(
                    "Unable to locate org.eclipse.tycho:org.eclipse.tycho.surefire.osgibooter and/or its fragments\n");
            sb.append("Test framework: " + testFramework);
            sb.append("All plugin artifacts: ");
            for (Artifact artifact : pluginArtifacts) {
                sb.append("\n\t").append(artifact.toString());
            }
            sb.append("\nMatched OSGi test booter artifacts: ");
            for (File file : result) {
                sb.append("\n\t").append(file.getAbsolutePath());
            }

            throw new MojoExecutionException(sb.toString());
        }

        return result;
    }

    private void createDevProperties(boolean includeReactorProjects, List<ReactorProject> reactorProjects)
            throws MojoExecutionException {
        Properties dev = new Properties();

        if (includeReactorProjects) {
            // this is needed for IDE integration, where we want to use reactor project output folders
            dev.put("@ignoredot@", "true");
            for (ReactorProject otherProject : reactorProjects) {
                if ("eclipse-test-plugin".equals(otherProject.getPackaging())
                        || "eclipse-plugin".equals(otherProject.getPackaging())) {
                    TychoProject projectType = projectTypes.get(otherProject.getPackaging());
                    dev.put(projectType.getArtifactKey(otherProject).getId(), getBuildOutputDirectories(otherProject));
                }
            }
        }

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);

        TychoProject projectType = projectTypes.get(project.getPackaging());
        dev.put(projectType.getArtifactKey(reactorProject).getId(), getBuildOutputDirectories(reactorProject));

        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(devProperties));
            try {
                dev.store(os, null);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't create osgi dev properties file", e);
        }
    }

    private String getBuildOutputDirectories(ReactorProject otherProject) {
        StringBuilder sb = new StringBuilder();

        sb.append(otherProject.getOutputDirectory());
        sb.append(',').append(otherProject.getTestOutputDirectory());

        BuildProperties buildProperties = buildPropertiesParser.parse(otherProject.getBasedir());
        for (Entry<String, String> outputEntry : buildProperties.getJarToOutputFolderMap().entrySet()) {
            if (".".equals(outputEntry.getKey())) {
                continue;
            }
            appendCommaSeparated(sb, outputEntry.getValue());
        }
        for (Entry<String, List<String>> sourceEntry : buildProperties.getJarToSourceFolderMap().entrySet()) {
            String fileName = sourceEntry.getKey();
            if (".".equals(fileName)) {
                continue;
            }
            String classesDir = otherProject.getBuildDirectory().getName() + "/"
                    + fileName.substring(0, fileName.length() - ".jar".length()) + "-classes";
            appendCommaSeparated(sb, classesDir);
        }
        return sb.toString();
    }

    private static void appendCommaSeparated(StringBuilder sb, String string) {
        if (sb.length() > 0) {
            sb.append(',');
        }
        sb.append(string);
    }

    private List<String> getBundlesToExplode() {
        List<String> bundles = new ArrayList<String>();

        if (explodedBundles != null) {
            bundles.addAll(Arrays.asList(explodedBundles));
        }

        return bundles;
    }

    private List<File> getFrameworkExtensions() throws MojoExecutionException {
        List<File> files = new ArrayList<File>();

        if (frameworkExtensions != null) {
            for (Dependency frameworkExtension : frameworkExtensions) {
                Artifact artifact = repositorySystem.createDependencyArtifact(frameworkExtension);
                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setResolveRoot(true).setResolveTransitively(false);
                request.setLocalRepository(session.getLocalRepository());
                request.setRemoteRepositories(project.getPluginArtifactRepositories());
                request.setOffline(session.isOffline());
                request.setForceUpdate(session.getRequest().isUpdateSnapshots());
                ArtifactResolutionResult result = repositorySystem.resolve(request);
                try {
                    resolutionErrorHandler.throwErrors(request, result);
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException("Failed to resolve framework extension "
                            + frameworkExtension.getManagementKey(), e);
                }
                files.add(artifact.getFile());
            }
        }

        return files;
    }

    public LaunchConfiguration createLaunchConfiguration(List<ReactorProject> reactorProjects) {
        try {
            EquinoxInstallation testRuntime = createEclipseInstallation(true, reactorProjects);

            return createCommandLine(testRuntime, work);
        } catch (MalformedURLException e) {
            getLog().error(e);
        } catch (MojoExecutionException e) {
            getLog().error(e);
        }

        return null;
    }
}
