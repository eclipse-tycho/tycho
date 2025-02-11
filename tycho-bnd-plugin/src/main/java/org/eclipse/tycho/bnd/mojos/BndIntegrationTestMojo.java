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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;

@Mojo(name = "integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class BndIntegrationTestMojo extends AbstractBndProjectMojo {

	/**
	 * Set this to <code>true</code> to bypass unit tests entirely. Its use is
	 * <b>NOT RECOMMENDED</b>, especially if you enable it using the
	 * "maven.test.skip" property, because maven.test.skip disables both running the
	 * tests and compiling the tests. Consider using the <code>skipTests</code>
	 * parameter instead that only skip the <i>execution</i> of tests.
	 */
	@Parameter(property = "maven.test.skip")
	private boolean skip;

	/**
	 * Set this to "true" to skip running tests, but still compile them. Its use is
	 * NOT RECOMMENDED, but quite convenient on occasion.
	 */
	@Parameter(property = "skipTests")
	private boolean skipTests;

	@Override
	protected void execute(Project project) throws Exception {
		if (skip) {
			return;
		}
		if (skipTests) {
			getLog().warn("Tests execution is skipped!");
			return;
		}
		String testcases = project.getProperty(Constants.TESTCASES);
		if (testcases == null) {
			return;
		}
		project.test();
	}

}
