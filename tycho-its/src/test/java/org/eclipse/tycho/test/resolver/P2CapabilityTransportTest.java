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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Test for OSGi capability resolution with Provide-Capability and Require-Capability.
 * This test replicates the scenario from eclipse-equinox/p2 PR #972 where bundles
 * use OSGi capabilities to express service dependencies.
 * 
 * The test creates two bundles that form a direct dependency cycle, exactly as it
 * happens between org.eclipse.equinox.p2.repository and org.eclipse.equinox.p2.transport.ecf:
 * - consumer.bundle: Requires the Transport capability (like org.eclipse.equinox.p2.repository)
 * - provider.bundle: Provides that capability, but also Require-Bundle's consumer.bundle
 *   (like org.eclipse.equinox.p2.transport.ecf, which Require-Bundle's org.eclipse.equinox.p2.repository)
 * 
 * Since consumer.bundle requires the capability provided by provider.bundle, and
 * provider.bundle requires the consumer.bundle itself, the two bundles form a direct
 * dependency cycle. Tycho cannot compute a reactor build order for a cyclic reference,
 * so the build is expected to fail.
 * 
 * @see <a href="https://github.com/eclipse-equinox/p2/pull/972">eclipse-equinox/p2#972</a>
 * @see <a href="https://github.com/eclipse-equinox/p2/issues/971">eclipse-equinox/p2#971</a>
 */
public class P2CapabilityTransportTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCapabilityProvideRequireCycleFails() throws Exception {
		Verifier verifier = getVerifier("/p2.capability.transport");
		try {
			verifier.executeGoal("verify");
		} catch (VerificationException expected) {
			//
		}
		verifier.verifyTextInLog("The projects in the reactor contain a cyclic reference");
	}

}
