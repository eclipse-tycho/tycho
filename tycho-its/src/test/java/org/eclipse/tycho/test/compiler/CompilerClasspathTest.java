/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompilerClasspathTest extends AbstractTychoIntegrationTest {

	@Test
	public void testPomOnlyDependencies() throws Exception {
		Verifier verifier = getVerifier("compiler.pomdependencies", true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Tests that transitive dependencies are added to the compiler classpath with
	 * forbidden access rules. This is needed when a bundle imports a package from
	 * another bundle whose types extend/implement types from a third bundle that is
	 * not directly imported. Without transitive deps on the classpath, the compiler
	 * cannot resolve the type hierarchy (e.g. forEach() from Iterable via an
	 * intermediate interface).
	 */
	@Test
	public void testTransitiveDependencyOnClasspath() throws Exception {
		Verifier verifier = getVerifier("compiler.transitiveDependency", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

}
