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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public abstract class AbstractBndProjectMojo extends AbstractBndMojo {

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Workspace wsp = getWorkspace();
			Project bndProject = wsp.getProject(mavenProject.getBasedir().getName());
			if (bndProject == null) {
				getLog().info("Not a bnd workspace project!");
				return;
			}
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

	protected abstract void execute(Project project) throws Exception;
}
