/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.junit.Test;

/**
 * Test for the tycho-compiler-plugin
 */
public class CompilerPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testJavac() throws Exception {
		Verifier verifier = getVerifier("tycho-compiler-plugin/javac", true, true);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

}
