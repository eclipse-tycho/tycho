/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class RunSingleTestTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("surefire.testSelection");

		// call test with -Dtest=bundle.WorkingTest -> supported since TYCHO-356
		verifier.addCliOption("-Dtest=bundle.WorkingTest");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testSingleMethod() throws Exception {
		Verifier verifier = getVerifier("surefire.testSelection");

		// the method suffix must reach the test provider, see
		// https://github.com/eclipse-tycho/tycho/issues/6217; MixedTest#broken throws, so a dropped
		// suffix fails the log check
		verifier.addCliOption("-Dtest=bundle.MixedTest#working");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Tests run: 1");
	}
}
