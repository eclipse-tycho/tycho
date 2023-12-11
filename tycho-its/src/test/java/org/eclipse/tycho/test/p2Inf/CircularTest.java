/*******************************************************************************
 * Copyright (c) 2023 Eclipse Contributors and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.p2Inf;

import static java.util.Arrays.asList;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 */
public class CircularTest extends AbstractTychoIntegrationTest {
	@Test
	public void testVirtualUnitRequirementDoesNotFailBuild() throws Exception {
		Verifier verifier = getVerifier("/p2Inf.circular", false);
		verifier.executeGoals(asList("verify"));
		verifier.verifyErrorFreeLog();
	}
}
