/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
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

package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.fail;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

public class RequireJREPackagesImportTest extends AbstractTychoIntegrationTest {

	@Ignore("TODO bug 514471 check if we can re-enable strict access rules for JDK packages")
	@Test
	public void testStrictImportJREPackages() throws Exception {
		Verifier verifier = getVerifier("compiler.requireJREPackageImports", false);
		try {
			verifier.executeGoal("compile");
			fail();
		} catch (VerificationException e) {
			// expected
			verifier.verifyTextInLog("[ERROR] Access restriction: The type 'InitialContext' is not API");
		}
	}
}
