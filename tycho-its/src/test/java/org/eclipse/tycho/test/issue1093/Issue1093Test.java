/*******************************************************************************
 * Copyright (c) 2022 Holger Voormann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tycho.test.issue1093;

import static org.junit.Assert.assertThrows;

import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Issue1093Test extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("issue1093", false);
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));
		// expected
		verifier.verifyTextInLog("bin.includes value(s) [non-existing-file.txt] do not match any files");
	}
}
