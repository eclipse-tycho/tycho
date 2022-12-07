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

public class SplitPackagesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testSplitPackage() throws Exception {
		Verifier verifier = getVerifier("/resolver.split/org.eclipse.equinox.security");
		verifier.addCliArgument("-Drepo=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

}
