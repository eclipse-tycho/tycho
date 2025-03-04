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

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import aQute.bnd.build.Project;

/**
 * Compile test sources
 */
@Mojo(name = "test-compile", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndTestCompileMojo extends AbstractBndCompileMojo {

	/**
	 * Set this to <code>true</code> to bypass compilation of test sources. Its use
	 * is <b>NOT RECOMMENDED</b>, but quite convenient on occasion.
	 */
	@Parameter(property = "maven.test.skip")
	private boolean skip;

	@Override
	protected void execute(Project project) throws Exception {
		if (skip) {
			return;
		}
		project.compile(true);
	}

	@Override
	protected Collection<File> getSourcePath(Project project) throws Exception {
		return List.of(project.getTestSrc());
	}

	@Override
	protected File getOutput(Project project) throws Exception {
		return project.getTestOutput();
	}

}
