/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype Inc. and others.
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

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class TestOptionalDependenciesTest extends AbstractTychoIntegrationTest {

	// tests that optional dependencies can be disabled in the test runtime in case
	// they are conflicting (cf. bug 351842)
	@Test
	public void testIgnoreMutuallyExclusiveOptionalDependenciesForTestRuntimeComputation() throws Exception {
		Verifier verifier = getVerifier("/surefire.optionalDependencies.ignore", false);
		verifier.addCliArgument("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	} // see also OptionalDependenciesTest.testOptionallyRequiredBundleCanBeIgnored()

	// tests that optionalDependencies configuration only affects the current
	// project (bug 367701)
	@Test
	public void reactorIndirectOptionalDependencies() throws Exception {
		Verifier verifier = getVerifier("/surefire.optionalDependencies.reactor", false);
		verifier.addCliArgument("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
