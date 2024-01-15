/*******************************************************************************
 * Copyright (c) 2018, 2023 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.assertNumberOfSuccessfulTests;
import static org.eclipse.tycho.test.util.SurefireUtil.assertTestMethodWasSuccessfullyExecuted;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JUnit5Test extends AbstractTychoIntegrationTest {

	@Test
	public void testJUnit5Runner() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit5/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "My 1st JUnit 5 test!");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"parameterizedJUnit5Test(String)[1] one");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"parameterizedJUnit5Test(String)[2] two");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"repeatedJUnit5Test() repetition 1 of 3");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"repeatedJUnit5Test() repetition 2 of 3");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"repeatedJUnit5Test() repetition 3 of 3");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit4Test", "testWithJUnit4");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit5Test", 6);
	}

	@Test
	public void testJUnit4and54Runner() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit4and54/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit4Test", "testWithJUnit4");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit54Test", "My 1st JUnit 5.4 test!");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit54Test", 1);
	}

	@Test
	public void testJUnit54Runner() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit54/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit54Test", "My 1st JUnit 5.4 test!");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit54Test", 1);
	}

	@Test
	public void testJUnit56Runner() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit56/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit56Test", "My 1st JUnit 5.6 test!");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit56Test", 1);
	}

	@Test
	public void testJUnit59Runner() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit59/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit59Test", "My 1st JUnit 5.9 test!");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit59Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[1] 0, 5, 5");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit59Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[2] 10, 10, 20");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit59Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[3] 12, 30, 42");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit59Test", 4);
	}

	@Test
	public void testJUnit59Suite() throws Exception {
		final Verifier verifier = getVerifier("/surefire.junit59suite/bundle.test");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "SuiteWithAllTests", "bundle.test.JUnit59Test",
				"started from test suite");
		// make sure tests from suite were executed
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit59Test", 1);
	}

}
