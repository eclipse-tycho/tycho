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

import static org.junit.Assert.fail;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * This test some nicer error messages if something is missing
 *
 */
public class TestErrorMessages extends AbstractTychoIntegrationTest {
	@Test
	public void testBREEWithoutProfile() throws Exception {
		// this will succeed as we have not selected an restrictive profile
		// it is actually a bit strange that it resolves at all, if you find this
		// test failing because of an improved handling in tycho feel free to remove it
		// as it actually should be equal to the plain profile
		Verifier verifier = getVerifier("compiler.messages/missing-bree", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Test error message with a 'plain' JVM
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingBREEWithPlainProfile() throws Exception {
		Verifier verifier = getVerifier("compiler.messages/missing-bree", false);
		verifier.addCliArgument("-Pplain");
		try {
			verifier.executeGoal("compile");
			fail();
		} catch (VerificationException e) {
			verifier.verifyTextInLog("java17.bundle 1.0.0 requires Execution Environment that matches");
			verifier.verifyTextInLog("but the current resolution context uses");
		}
	}

	/**
	 * Test error message with a justj vm and executionEnvironment=NONE
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingBREEWithJustJProfile() throws Exception {
		Verifier verifier = getVerifier("compiler.messages/missing-bree", false);
		verifier.addCliArgument("-Pjustj");
		try {
			verifier.executeGoal("compile");
			fail();
		} catch (VerificationException e) {
			verifier.verifyTextInLog(
					"The following Execution Environments are currently known but are ignored by configuration");
		}
	}
}
