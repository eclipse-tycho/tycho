/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.junit.Assert.fail;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class InvalidProductTest extends AbstractTychoIntegrationTest {

	@Test
	public void testInvalidProductFile() throws Exception {
		Verifier verifier = getVerifier("product.invalid", false);
		verifier.addCliArgument("-Dtest-data-repo=" + P2Repositories.ECLIPSE_342.toString());

		// run build and verify we get a proper error message instead of an NPE
		try {
			verifier.executeGoal("package");
			fail("We expect to fail on malformed product definitions");
		} catch (VerificationException e) {
			verifier.verifyTextInLog("The product file invalid.product does not contain the mandatory attribute");
		}
	}
}
