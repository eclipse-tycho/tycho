/*******************************************************************************
 * Copyright (c) 2022 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CycleTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDependencyCycle() throws Exception {
		Verifier verifier = getVerifier("/resolver.cycles");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

}
