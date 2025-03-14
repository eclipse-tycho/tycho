/*******************************************************************************
 * Copyright (c) 2025 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.testResultFile;
import static org.junit.Assert.assertTrue;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Junit4ContextClassloaderTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {

		// a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
		Verifier verifier = getVerifier("surefire.junit4/contextclassloader.test");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertTrue(testResultFile(verifier.getBasedir(), "contextclassloader.test", "JUnit4Test").exists());

	}
}
