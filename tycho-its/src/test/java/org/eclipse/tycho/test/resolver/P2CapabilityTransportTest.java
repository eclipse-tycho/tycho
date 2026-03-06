/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Test for OSGi capability resolution with Provide-Capability and Require-Capability.
 * This test replicates the scenario from eclipse-equinox/p2 PR #972 where bundles
 * use OSGi capabilities to express service dependencies.
 * 
 * The test creates two bundles:
 * - provider.bundle: Provides a capability for Transport service
 * - consumer.bundle: Requires that capability
 * 
 * This ensures Tycho can correctly resolve bundles with capability requirements.
 * 
 * @see <a href="https://github.com/eclipse-equinox/p2/pull/972">eclipse-equinox/p2#972</a>
 */
public class P2CapabilityTransportTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCapabilityProvideRequire() throws Exception {
		Verifier verifier = getVerifier("/p2.capability.transport");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
