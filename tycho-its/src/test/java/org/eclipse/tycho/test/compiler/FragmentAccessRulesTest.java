/*******************************************************************************
 * Copyright (c) 2026 Chris Lake and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Lake - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * The platform specific SWT fragment provides the internal packages of the host bundle. Its access
 * rules must still be applied during compilation.
 */
public class FragmentAccessRulesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSuppressedRestrictionIsNotReportedAsUnnecessary() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments.accessRules", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
		verifyTextNotInLog(verifier, "Unnecessary @SuppressWarnings(\"restriction\")");
	}

	@Test
	public void testInternalApiOfFragmentIsDiscouraged() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments.accessRules", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Unsuppressed.java");
		verifier.verifyTextInLog("Discouraged access: The type 'DPIUtil' is not API");
	}

	@Test
	public void testFragmentFromTargetIsOnClasspathOfItsHost() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments.hostClasspath", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}
}
