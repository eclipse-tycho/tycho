/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsetest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.EclipseFramework;
import org.eclipse.tycho.osgi.framework.EclipseWorkspaceManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public abstract class AbstractEclipseTestMojo extends AbstractMojo {

	private static final String PARAMETER_JUNIT_REPORT_OUTPUT = "junitReportOutput";

	private static final String PARAMETER_CLASSNAME = "classname";

	private static final String PARAMETER_TESTPLUGINNAME = "testpluginname";

	static final String PARAMETER_LOCAL = "local";

	private static final String NAME = "Eclipse Test";

	@Parameter()
	private Repository eclipseRepository;

	@Parameter(defaultValue = "false", property = "tycho.eclipsetest.skip")
	private boolean skip;

	@Parameter(defaultValue = "false", property = "tycho.eclipsetest.debug")
	private boolean debug;

	@Parameter(name = PARAMETER_CLASSNAME, alias = "test-classname", required = true)
	private String classname;

	@Parameter(name = PARAMETER_JUNIT_REPORT_OUTPUT)
	private String junitReportOutput;

	@Parameter(defaultValue = "${project.build.directory}/eclipse-test-reports/${project.artifactId}.xml")
	private File resultFile;

	@Parameter
	private List<String> bundles;

	@Parameter
	private List<String> features;

	/**
	 * Controls if the local target platform of the project should be used to
	 * resolve the eclipse application
	 */
	@Parameter(defaultValue = "false", property = "tycho.eclipsebuild.local", name = PARAMETER_LOCAL)
	private boolean local;

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Component
	private EclipseWorkspaceManager workspaceManager;

	@Component
	private EclipseApplicationManager eclipseApplicationManager;

	@Component
	private TychoProjectManager projectManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		EclipseApplication application;
		Bundles bundles = new Bundles(getBundles());
		Features features = new Features(getFeatures());
		if (local) {
			TargetPlatform targetPlatform = projectManager.getTargetPlatform(project).orElseThrow(
					() -> new MojoFailureException("Can't get target platform for project " + project.getId()));
			application = eclipseApplicationManager.getApplication(targetPlatform, bundles, features, NAME);
		} else {
			application = eclipseApplicationManager.getApplication(eclipseRepository, bundles, features, NAME);
		}
		List<String> arguments = new ArrayList<>();
		arguments.add(EclipseApplication.ARG_APPLICATION);
		arguments.add(getApplication());
		arguments.add(toParam(PARAMETER_TESTPLUGINNAME));
		arguments.add(projectManager.getArtifactKey(project).get().getId());
		arguments.add(toParam(PARAMETER_CLASSNAME));
		arguments.add(classname);
		if (junitReportOutput != null) {
			arguments.add(toParam(PARAMETER_JUNIT_REPORT_OUTPUT));
			arguments.add(junitReportOutput);
		}
		arguments.add("formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,"
				+ resultFile.getAbsolutePath());
		try (EclipseFramework framework = application.startFramework(workspaceManager
				.getWorkspace(EclipseApplicationManager.getRepository(eclipseRepository).getURL(), this), arguments)) {
			if (debug) {
				framework.printState();
			}
			Bundle install = framework.install(project.getArtifact().getFile());
			try {
				install.start();
				framework.start();
			} finally {
				install.uninstall();
			}
		} catch (BundleException e) {
			throw new MojoFailureException("Can't start framework!", e);
		} catch (Exception e) {
			throw new MojoExecutionException(e);
		}
	}

	private Set<String> getBundles() {
		Set<String> bundles = new HashSet<String>();
		bundles.add("org.eclipse.test");
		return bundles;
	}

	private Set<String> getFeatures() {
		Set<String> set = new HashSet<>();
		if (features != null) {
			set.addAll(features);
		}
		return set;
	}

	protected abstract String getApplication();

	private static String toParam(String name) {
		return "-" + name;
	}
}
