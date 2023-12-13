/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.junit.Assert.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FailIfNoTestsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testNoTestsNoFailure() throws Exception {
		Verifier verifier = getVerifier("surefire.noTests");

		// support for this option was requested in TYCHO-432
		verifier.addCliOption("-DfailIfNoTests=false");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testNoTestsFailureDefaultCase() throws Exception {
		Verifier verifier = getVerifier("surefire.noTests");
		assertThrows(VerificationException.class, () -> verifier.executeGoal("integration-test"));
		verifier.verifyTextInLog("No tests found");
	}

}
