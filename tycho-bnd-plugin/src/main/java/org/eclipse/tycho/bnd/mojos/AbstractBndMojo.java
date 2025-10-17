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
 *******************************************************************************/
package org.eclipse.tycho.bnd.mojos;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.bnd.MavenReactorRepository;

import aQute.bnd.build.Workspace;
import aQute.service.reporter.Report;

public abstract class AbstractBndMojo extends AbstractMojo {

	@Inject
	protected MavenProject mavenProject;

	@Inject
	protected MavenReactorRepository mavenReactorRepository;

	protected Workspace getWorkspace() throws MojoFailureException {
		try {
			Workspace workspace = Workspace.getWorkspace(mavenProject.getBasedir().getParentFile());
			if (workspace.getPlugins().stream().noneMatch(plugin -> plugin instanceof MavenReactorRepository)) {
				workspace.addBasicPlugin(mavenReactorRepository);
				workspace.refresh();
			}
			checkResult(workspace, workspace.isFailOk());
			return workspace;
		} catch (Exception e) {
			throw new MojoFailureException("error while locating workspace!", e);
		}
	}

	protected void checkResult(Report report, boolean errorOk) throws MojoFailureException {
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

}
