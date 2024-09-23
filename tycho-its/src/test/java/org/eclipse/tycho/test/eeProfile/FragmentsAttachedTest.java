/*******************************************************************************
 * Copyright (c) 2014, 2021 SAP SE and others.
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
package org.eclipse.tycho.test.eeProfile;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class FragmentsAttachedTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSWTFragmentsAttached() throws Exception {
		Verifier verifier = getVerifier("eeProfile.resolution.fragments", false);

		verifier.executeGoal("verify");

		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testSWTFragmentsAttached_unmatchingFromP2() throws Exception {
		Verifier verifier = getVerifier("eeProfile.resolution.fragments.unmatchinginp2", false);

		assertThrows(VerificationException.class, () -> verifier.executeGoal("verify"));
	}

}
