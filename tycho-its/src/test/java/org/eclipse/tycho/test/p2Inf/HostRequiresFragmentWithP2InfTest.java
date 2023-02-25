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
package org.eclipse.tycho.test.p2Inf;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class HostRequiresFragmentWithP2InfTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBuildAndTestWithHostRequiringOwnFragment() throws Exception {
		Verifier verifier = getVerifier("/p2Inf.hostRequireFragment", false);
		verifier.addCliArgument("-Dp2Repository=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST);

		// Test that
		// - the install-time dependency from the host to its fragment can be disabled
		// at build
		// time through a resolver property (feature 405440),
		// - the install-time dependency is in effect for the consumer of the host (bug
		// 441113),
		// i.e. the dependency resolution identifies the fragment as transitive
		// dependency of
		// the consumer and hence it is added to the final target platform of the
		// consumer, and
		// the test runtime resolver adds the fragment to the test runtime.
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("Running phf.consumer.HostInterfaceUsageTest");
	}
}
