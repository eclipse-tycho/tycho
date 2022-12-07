/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.compiler;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompilerClasspathTest extends AbstractTychoIntegrationTest {

	@Test
	public void testPomOnlyDependencies() throws Exception {
		Verifier verifier = getVerifier("compiler.pomdependencies", true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

}
