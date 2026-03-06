/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.testResultFile;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestNGBundleTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {

		Verifier verifier = getVerifier("surefire.testng");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertTrue(testResultFile(verifier.getBasedir() + File.separator + "bundle.test", "bundle.test", "TestNGTest")
				.exists());

		assertTrue(testResultFile(verifier.getBasedir() + File.separator + "bundle.testGroups", "bundle.test",
				"GroupsTest").exists());

		assertTrue(testResultFile(verifier.getBasedir() + File.separator + "bundle.testSuites", "TestSuite").exists());

	}

}
