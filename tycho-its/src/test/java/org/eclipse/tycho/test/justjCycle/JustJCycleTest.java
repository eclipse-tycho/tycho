/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.justjCycle;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JustJCycleTest extends AbstractTychoIntegrationTest {

	@Test
	public void testCustomBundleParent() throws Exception {
		Verifier verifier = getVerifier("justj-cycle");
		verifier.setForkJvm(false);
		verifier.setSystemProperty("user.home", System.getProperty("user.home"));
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}
}
