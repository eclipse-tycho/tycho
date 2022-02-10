/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.surefire;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestsInBundleTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCompile() throws Exception {
		Verifier verifier = getVerifier("surefire.combinedtests/bundle.test");
		verifier.executeGoals(Arrays.asList("clean", "test-compile"));
		verifier.verifyErrorFreeLog();
		assertTrue("compiled class file does not exist",
				new File(verifier.getBasedir(), "target/classes/bundle/test/Counter.class").exists());
		assertTrue("compiled test-class file does not exist",
				new File(verifier.getBasedir(), "target/test-classes/bundle/test/AdderTest.class").exists());
	}

	@Test
	public void testTest() throws Exception {
		Verifier verifier = getVerifier("surefire.combinedtests/bundle.test");
		verifier.executeGoals(Arrays.asList("clean", "test"));
		verifier.verifyErrorFreeLog();
		assertTrue("tests were not run",
				new File(verifier.getBasedir(), "target/surefire-reports/bundle.test.AdderTest.txt").exists());
	}

	@Test
	public void testIntegrationTest() throws Exception {
		Verifier verifier = getVerifier("surefire.combinedtests/bundle.test");
		verifier.executeGoals(Arrays.asList("clean", "integration-test"));
		verifier.verifyErrorFreeLog();
		assertTrue("summary report not found",
				new File(verifier.getBasedir(), "target/failsafe-reports/failsafe-summary.xml").exists());
		verifier.verifyTextInLog("Tests run: 2, Failures: 1, Errors: 0, Skipped: 0");
		verifier.verifyTextInLog("OSGiRunningIT.willFail:30 This fail is intentional");
	}

	@Test
	public void testVerify() throws Exception {
		Verifier verifier = getVerifier("surefire.combinedtests/bundle.test");
		try {
			verifier.executeGoals(Arrays.asList("clean", "verify"));
			fail("the build succeed but test-failures are expected!");
		} catch (VerificationException e) {
			// thats good indeed...
			verifier.verifyTextInLog("There are test failures");
		}
	}

}
