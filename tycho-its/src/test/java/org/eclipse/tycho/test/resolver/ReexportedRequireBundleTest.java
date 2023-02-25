/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class ReexportedRequireBundleTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBundleNativeCode() throws Exception {
		Verifier verifier = getVerifier("/resolver.reexportBundle/transitively.require.org.eclipse.osgi");
		verifier.addCliArgument("-Drepo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testOrgEclipseCoreExpressions() throws Exception {
		Verifier verifier = getVerifier("/resolver.reexportBundle/org.eclipse.core.expressions");
		verifier.addCliArgument("-Drepo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}
}
