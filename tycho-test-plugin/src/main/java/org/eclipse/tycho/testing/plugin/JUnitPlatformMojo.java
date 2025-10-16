/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.testing.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.osgi.OSGiFramework;
import org.eclipse.tycho.osgi.OSGiFrameworkLauncher;
import org.osgi.framework.Constants;

/**
 * Execute tests using the JUnit Platform inside an OSGi Framework using the
 * <a href=
 * "https://docs.junit.org/current/user-guide/#running-tests-console-launcher">tests-console-launcher</a>
 */
@Mojo(name = "junit-platform", threadSafe = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresProject = true, requiresDependencyCollection = ResolutionScope.TEST)
public class JUnitPlatformMojo extends AbstractMojo {

	@Component
	TychoProjectManager projectManager;

	@Component
	MavenProject mavenProject;

	@Component
	Map<String, OSGiFrameworkLauncher> launchers;

	/**
	 * Select specific test classes to execute. Each entry should be a fully
	 * qualified class name (e.g., "com.example.MyTest").
	 */
	@Parameter(property = "select-class")
	private List<String> selectClass;

	/**
	 * Select specific test methods to execute. Each entry should be a fully
	 * qualified method name (e.g., "com.example.MyTest#myTestMethod").
	 */
	@Parameter(property = "select-method")
	private List<String> selectMethod;

	/**
	 * Select packages to scan for tests.
	 */
	@Parameter(property = "select-package")
	private List<String> selectPackage;

	/**
	 * Select specific Java modules for test discovery.
	 */
	@Parameter(property = "select-module")
	private List<String> selectModule;

	/**
	 * Select classpath resources for test discovery.
	 */
	@Parameter(property = "select-resource")
	private List<String> selectResource;

	/**
	 * Select URIs for test discovery.
	 */
	@Parameter(property = "select-uri")
	private List<String> selectUri;

	@Parameter(property = "scan-classpath", defaultValue = "true")
	private boolean scanClasspath;

	@Parameter(property = "disable-banner", defaultValue = "true")
	private boolean disableBanner;

	@Parameter(property = "reports-dir", defaultValue = "${project.build.directory}/testReports")
	private File reportsDir;

	/**
	 * Regular expression to include test classes by name.
	 */
	@Parameter(property = "include-classname")
	private String includeClassname;

	/**
	 * Regular expression to exclude test classes by name.
	 */
	@Parameter(property = "exclude-classname")
	private String excludeClassname;

	/**
	 * Package names to include in test execution.
	 */
	@Parameter(property = "include-package")
	private List<String> includePackage;

	/**
	 * Package names to exclude from test execution.
	 */
	@Parameter(property = "exclude-package")
	private List<String> excludePackage;

	/**
	 * Test engine IDs to include (e.g., "junit-jupiter", "junit-vintage").
	 */
	@Parameter(property = "include-engine")
	private List<String> includeEngine;

	/**
	 * Test engine IDs to exclude.
	 */
	@Parameter(property = "exclude-engine")
	private List<String> excludeEngine;

	/**
	 * Tags to include in test execution.
	 */
	@Parameter(property = "include-tag")
	private List<String> includeTag;

	/**
	 * Tags to exclude from test execution.
	 */
	@Parameter(property = "exclude-tag")
	private List<String> excludeTag;

	/**
	 * Fail and return exit status code 2 if no tests are found.
	 */
	@Parameter(property = "fail-if-no-tests", defaultValue = "true")
	private boolean failIfNoTests;

	/**
	 * Tree printing mode for test output. Valid values: none, summary, flat,
	 * tree, verbose.
	 */
	@Parameter(property = "details")
	private String details;

	/**
	 * ASCII art theme for tree printing. Valid values: ascii, unicode.
	 */
	@Parameter(property = "details-theme")
	private String detailsTheme;

	/**
	 * Style test output in single color (no ANSI color codes).
	 */
	@Parameter(property = "single-color", defaultValue = "false")
	private boolean singleColor;

	@Parameter
	private Map<String, String> config;

	/**
	 * Additional OSGi framework properties to use when executing the test
	 */
	@Parameter
	private Map<String, String> frameworkProperties = new LinkedHashMap<>();

	/**
	 * The initial start level used for bundles and the OSGi framework.
	 */
	@Parameter(defaultValue = "4")
	private int applicationStartLevel;

	/**
	 * The start level used for bundles providing an <code>osgi.extender</code>
	 * capability.
	 */
	@Parameter(defaultValue = "1")
	private int extenderStartLevel;

	/**
	 * If bundles providing an <code>osgi.extender</code> capability should be
	 * started by default
	 */
	@Parameter(defaultValue = "true")
	private boolean startExtender;

	/**
	 * Value in seconds how long to wait until the framework reaches the final
	 * startlevel
	 */
	@Parameter(defaultValue = "30")
	private long startupTimout;

	@Parameter
	private boolean printBundles;

	@Parameter
	private boolean printDebug;

	@Parameter(property = "junit-platform.skip")
	private boolean skip;

	@Parameter(defaultValue = "embedded", property = "junit-platform.launchType")
	private String launchType;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		if (skip) {
			log.info("Execution is skipped!");
			return;
		} else {
			log.info("Reports will be written to: " + reportsDir);
		}
		Collection<Path> projectDependencies;
		try {
			projectDependencies = projectManager.getProjectDependencies(mavenProject);
		} catch (Exception e) {
			throw new MojoExecutionException("Can't determine project dependencies!");
		}
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		for (Artifact artifact : artifacts) {
			if (Artifact.SCOPE_TEST.equals(artifact.getScope()) && artifact.getArtifactHandler().isAddedToClasspath()) {
				log.debug("Adding test scoped artifact: " + artifact);
				projectDependencies.add(artifact.getFile().toPath());
			}
		}
		log.debug("Project Dependencies:");
		for (Path path : projectDependencies) {
			log.debug(path.getFileName().toString());
		}
		JUnitPlatformRunnerResult runnerResult;
		Path workDir = getWorkDir();
		try (OSGiFramework framework = launchFramework(workDir)) {
			try {
				runnerResult = framework.runInFramework(
						new JUnitPlatformRunner(projectDependencies, mavenProject.getArtifact().getFile().toPath(),
								applicationStartLevel,
								extenderStartLevel, startExtender, startupTimout, printBundles, buildArguments(),
								workDir));
			} catch (IOException | RuntimeException e) {
				throw new MojoExecutionException("Execute runner failed!", e);
			}
		}
		runnerResult.infoMessages().forEach(log::info);
		if (log.isDebugEnabled()) {
			runnerResult.debugMessages().forEach(log::debug);
		} else if (printDebug) {
			runnerResult.debugMessages().forEach(log::info);
		}
		Exception exception = runnerResult.getException();
		if (exception == null) {
			return;
		}
		if (exception instanceof JUnitPlatformFailureException jf) {
			throw new MojoExecutionException(jf.getMessage(), jf.getCause());
		} else {
			throw new MojoExecutionException("Runner execution failed!", exception);
		}
	}

	private OSGiFramework launchFramework(Path workDir) throws MojoExecutionException {
		OSGiFrameworkLauncher launcher = launchers.get(launchType);
		if (launcher == null) {
			throw new MojoExecutionException("Launch type '" + launchType + "' is not available");
		}
		OSGiFramework framework;
		try {
			framework = launcher.launchFramework(mavenProject, Map.of( //
					Constants.FRAMEWORK_STORAGE, workDir.toAbsolutePath().toString(),
					Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(1),
					OSGiFrameworkLauncher.STANDALONE, "true")
			);
		} catch (Exception e) {
			throw new MojoExecutionException("Can't launch '" + launchType + "' framework!", e);
		}
		return framework;
	}

	private Path getWorkDir() {
		return Path.of(mavenProject.getBuild().getDirectory())
				.resolve("junit-platform-work");
	}

	private List<String> buildArguments() {
		List<String> arguments = new ArrayList<>();
		arguments.add("execute");
		if (disableBanner) {
			arguments.add("--disable-banner");
		}
		if (selectClass != null) {
			for (String clz : selectClass) {
				arguments.add("--select-class");
				arguments.add(clz);
			}
		}
		if (selectMethod != null) {
			for (String method : selectMethod) {
				arguments.add("--select-method");
				arguments.add(method);
			}
		}
		if (selectPackage != null) {
			for (String pkg : selectPackage) {
				arguments.add("--select-package");
				arguments.add(pkg);
			}
		}
		if (selectModule != null) {
			for (String module : selectModule) {
				arguments.add("--select-module");
				arguments.add(module);
			}
		}
		if (selectResource != null) {
			for (String resource : selectResource) {
				arguments.add("--select-resource");
				arguments.add(resource);
			}
		}
		if (selectUri != null) {
			for (String uri : selectUri) {
				arguments.add("--select-uri");
				arguments.add(uri);
			}
		}
		if (scanClasspath) {
			arguments.add("--scan-classpath");
			arguments.add(mavenProject.getArtifact().getFile().getAbsolutePath());
		}
		if (includeClassname != null) {
			arguments.add("--include-classname");
			arguments.add(includeClassname);
		}
		if (excludeClassname != null) {
			arguments.add("--exclude-classname");
			arguments.add(excludeClassname);
		}
		if (includePackage != null) {
			for (String pkg : includePackage) {
				arguments.add("--include-package");
				arguments.add(pkg);
			}
		}
		if (excludePackage != null) {
			for (String pkg : excludePackage) {
				arguments.add("--exclude-package");
				arguments.add(pkg);
			}
		}
		if (includeEngine != null) {
			for (String engine : includeEngine) {
				arguments.add("--include-engine");
				arguments.add(engine);
			}
		}
		if (excludeEngine != null) {
			for (String engine : excludeEngine) {
				arguments.add("--exclude-engine");
				arguments.add(engine);
			}
		}
		if (includeTag != null) {
			for (String tag : includeTag) {
				arguments.add("--include-tag");
				arguments.add(tag);
			}
		}
		if (excludeTag != null) {
			for (String tag : excludeTag) {
				arguments.add("--exclude-tag");
				arguments.add(tag);
			}
		}
		if (config != null) {
			for (Entry<String, String> entry : config.entrySet()) {
				arguments.add("--config");
				arguments.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
			}
		}
		if (details != null) {
			arguments.add("--details");
			arguments.add(details);
		}
		if (detailsTheme != null) {
			arguments.add("--details-theme");
			arguments.add(detailsTheme);
		}
		if (singleColor) {
			arguments.add("--single-color");
		}
		arguments.add("--reports-dir");
		arguments.add(reportsDir.getAbsolutePath());
		if (failIfNoTests) {
			arguments.add("--fail-if-no-tests");
		}
		return arguments;
	}


}
