/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.pomDependencyConsider;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomDependencyIgnoreTest extends AbstractTychoIntegrationTest {

	@Test
	public void testIgnorePomDependency() throws Exception {
		Verifier verifier = getVerifier("pomDependency.ignore", true, true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}
