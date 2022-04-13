/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompilerClasspathEntryTest extends AbstractTychoIntegrationTest {

	@Test
	public void testJUnit4Container() throws Exception {
		Verifier verifier = getVerifier("compiler.junitcontainer/junit4-in-bundle", true);
		verifier.executeGoal("test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testJUnit4ContainerWithDependencies() throws Exception {
		Verifier verifier = getVerifier("compiler.junitcontainer/junit4-in-bundle-with-dependencies", true);
		verifier.executeGoal("test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testLibEntry() throws Exception {
		Verifier verifier = getVerifier("compiler.libentry/my.bundle", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testDSComponents() throws Exception {
		Verifier verifier = getVerifier("tycho-ds", false, true);
		verifier.setSystemProperty("repo-url", "https:////download.eclipse.org/releases/2022-03/");
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File generated = new File(verifier.getBasedir(), "target/classes/OSGI-INF");
		assertTrue(new File(generated, "tycho.ds.TestComponent.xml").isFile());
		assertFalse(new File(generated, "tycho.ds.TestComponent2.xml").isFile());
	}

}
