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
package org.eclipse.tycho.cleancode;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuildMojo;
import org.eclipse.tycho.model.project.EclipseProject;

/**
 * This mojo allows to perform eclipse cleanup action
 */
@Mojo(name = "cleanup", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CleanUpMojo extends AbstractEclipseBuildMojo<CleanupResult> {

	@Parameter(defaultValue = "${project.build.directory}/cleanups.md", property = "tycho.cleanup.report")
	private File reportFileName;

	/**
	 * Defines key value pairs of a cleanup profile, if not defined will use the
	 * project defaults
	 */
	@Parameter
	private Map<String, String> cleanUpProfile;

	@Override
	protected String[] getRequireBundles() {
		return new String[] { "org.eclipse.jdt.ui" };
	}

	@Override
	protected String getName() {
		return "Perform Cleanup";
	}

	@Override
	protected CleanUp createExecutable() {
		return new CleanUp(project.getBasedir().toPath(), debug, cleanUpProfile);
	}

	@Override
	protected void handleResult(CleanupResult result)
			throws MojoFailureException {
		if (result.isEmpty()) {
			return;
		}
		MarkdownBuilder builder = new MarkdownBuilder(reportFileName);
		builder.h3("The following cleanups where applied:");
		result.cleanups().forEach(cleanup -> {
			builder.addListItem(cleanup);
		});
		builder.newLine();
		builder.newLine();
		builder.write();
	}

	@Override
	protected boolean isValid(EclipseProject eclipseProject) {
		// Cleanups can only be applied to java projects
		return eclipseProject.hasNature("org.eclipse.jdt.core.javanature");
	}

}
