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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.bnd.mojos;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.service.reporter.Report;

public abstract class AbstractBndProjectMojo extends AbstractMojo {

	@Parameter(property = "project", readonly = true)
	protected MavenProject mavenProject;

	@Parameter(property = "session", readonly = true)
	protected MavenSession session;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Project bndProject = Workspace.getProject(mavenProject.getBasedir());
			Workspace workspace = bndProject.getWorkspace();
			checkResult(workspace, workspace.isFailOk());
			synchronized (bndProject) {
				execute(bndProject);
				checkResult(bndProject, bndProject.isFailOk());
			}
		} catch (Exception e) {
			if (e instanceof MojoFailureException mfe) {
				throw mfe;
			}
			if (e instanceof MojoExecutionException mee) {
				throw mee;
			}
			if (e instanceof RuntimeException rte) {
				throw rte;
			}
			throw new MojoExecutionException(e);
		}

	}

	private void checkResult(Report report, boolean errorOk) throws MojoFailureException {
		List<String> warnings = report.getWarnings();
		for (String warning : warnings) {
			getLog().warn(warning);
		}
		warnings.clear();
		List<String> errors = report.getErrors();
		for (String error : errors) {
			getLog().error(error);
		}
		if (errorOk) {
			errors.clear();
			return;
		}
		if (errors.size() > 0) {
			throw new MojoFailureException(errors.stream().collect(Collectors.joining(System.lineSeparator())));
		}
	}

	protected abstract void execute(Project project) throws Exception;
}
