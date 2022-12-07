/*******************************************************************************
 * Copyright (c) 2022 itemis AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     itemis AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho937;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho937Test extends AbstractTychoIntegrationTest {

	@Test
	public void testCompilerSourceTargetConfigurationViaManifest() throws Exception {
		Verifier verifier = getVerifier("tycho937", false);
		verifier.executeGoals(List.of("clean", "javadoc:aggregate-jar", "verify"));
		verifier.verifyErrorFreeLog();
	}

}
