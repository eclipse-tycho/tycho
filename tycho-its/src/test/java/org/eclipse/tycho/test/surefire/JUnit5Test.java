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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JUnit5Test extends AbstractTychoIntegrationTest {

	/**
	 * This tests basic JUnit operations (using latest version)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJUnit5() throws Exception {
		final Verifier verifier = getVerifier("/tycho-surefire-plugin/junit5/basic");
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

	/**
	 * Verifies basic compatibility with previous JUnit version, as JUnit 5 slightly
	 * varies in its metadata we need to test with a real eclipse release containing
	 * the JUnit version to make sure Tycho can still be used to test with older
	 * releases.
	 * 
	 * @param version
	 * @throws Exception
	 */
	@ParameterizedTest(name = "JUnit {1}")
	@ValueSource(strings = { "5.8", "5.9", "5.12" })
	public void testJUnitCompatibility(String version) throws Exception {
		final Verifier verifier = getVerifier("/tycho-surefire-plugin/junit5/compatibility/" + version);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		final String projectBasedir = verifier.getBasedir();
		verifyRunner(projectBasedir + "/runner");
		verifySuite(projectBasedir + "/suite");
		verifyVintage(projectBasedir + "/vintage");
	}

	private void verifyVintage(final String projectBasedir) throws Exception {
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit4Test", "testWithJUnit4");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "My 1st JUnit 5 test!");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit5Test", 1);
	}

	private void verifyRunner(final String projectBasedir) throws Exception {
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "My 1st JUnit 5 test!");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[1] 0, 5, 5");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[2] 10, 10, 20");
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
				"parameterizedJUnit59TestWithMethodSource(int, int, int)[3] 12, 30, 42");
		// make sure test tagged as 'slow' was skipped
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit5Test", 4);
	}

	private void verifySuite(final String projectBasedir) throws Exception {
		assertTestMethodWasSuccessfullyExecuted(projectBasedir, "SuiteWithAllTests", "bundle.test.JUnit5Test",
				"started from test suite");
		// make sure tests from suite were executed
		assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit5Test", 1);
	}

}
