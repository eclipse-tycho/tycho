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
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.build.BuildListener;
import org.eclipse.tycho.core.maven.MavenDependencyInjector;
import org.eclipse.tycho.helper.PluginConfigurationHelper;
import org.eclipse.tycho.helper.PluginConfigurationHelper.Configuration;
import org.eclipse.tycho.helper.ProjectHelper;

@Named("javadoc")
@Singleton
public class JavadocBuildListener implements BuildListener {

	private static final String JAVADOC_GOAL = "javadoc";
	private static final String ARTIFACT_ID = "maven-javadoc-plugin";
	private static final String GROUP_ID = "org.apache.maven.plugins";

	@Inject
	private ProjectHelper projectHelper;

	@Inject
	private PluginConfigurationHelper configurationHelper;

	@Override
	public void buildStarted(MavenSession session) {
		List<MavenProject> projects = session.getProjects();
		for (MavenProject project : projects) {
			if (isJavadocProject(project, session)) {
				Configuration configuration = configurationHelper.getConfiguration(GROUP_ID, ARTIFACT_ID, JAVADOC_GOAL,
						project, session);
				List<MavenProject> additionalProjects = configuration.getString("sourcepath").stream()
						.flatMap(sourcepath -> {
							return Arrays.stream(sourcepath.split(";|:"));
						}).map(s -> s.strip()).map(s -> new File(project.getBasedir(), s).toPath().normalize())
						.map(sourcePath -> getProject(sourcePath, projects)).filter(Objects::nonNull).distinct()
						.toList();
				MavenDependencyInjector.injectMavenProjectDependencies(project, additionalProjects);
			}
		}

	}

	private MavenProject getProject(Path sourcePath, List<MavenProject> projects) {
		MavenProject match = null;
		int matchNameCount = -1;
		for (MavenProject mavenProject : projects) {
			Path basePath = mavenProject.getBasedir().toPath();
			if (sourcePath.startsWith(basePath)) {
				int nameCount = basePath.getNameCount();
				if (match == null || nameCount > matchNameCount) {
					match = mavenProject;
					matchNameCount = nameCount;
				}
			}
		}
		return match;
	}

	private boolean isJavadocProject(MavenProject project, MavenSession mavenSession) {
		if (projectHelper.hasPluginExecution(GROUP_ID, ARTIFACT_ID, JAVADOC_GOAL, project, mavenSession)) {
			Configuration configuration = configurationHelper.getConfiguration(ConfigureMojo.class, project,
					mavenSession);
			return configuration.getBoolean(ConfigureMojo.PARAM_INJECT_JAVADOC_DEPENDENCIES).orElse(false);
		}
		return false;
	}

	@Override
	public void buildEnded(MavenSession session) {
		// nothing to do...
	}

}
