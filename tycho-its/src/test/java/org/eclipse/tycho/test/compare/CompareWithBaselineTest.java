/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tycho.test.compare;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompareWithBaselineTest extends AbstractTychoIntegrationTest {

	@Test
	public void testEENone() throws Exception {
		Verifier verifier = getVerifier("eeProfile.none/feature", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}