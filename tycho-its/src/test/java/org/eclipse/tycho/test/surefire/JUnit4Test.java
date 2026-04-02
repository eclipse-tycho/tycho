/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
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

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JUnit4Test extends AbstractTychoIntegrationTest {

	@Test
	public void basicTest() throws Exception {

		// a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
		Verifier verifier = getVerifier("tycho-surefire-plugin/junit4/bundle.test");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertTrue(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit4Test").exists());

		// ensure that JUnit 3 style tests also work -> related to bug 388909
		assertTrue(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit3Test").exists());
	}

	@Test
	public void contextClassLoaderTest() throws Exception {

		// a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
		Verifier verifier = getVerifier("tycho-surefire-plugin/junit4/contextclassloader.test/");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		File testResultFile = testResultFile(verifier.getBasedir(), "contextclassloader.test", "JUnit4Test");
		assertTrue("Test Result File" + testResultFile + " is missing", testResultFile.exists());

	}

}
