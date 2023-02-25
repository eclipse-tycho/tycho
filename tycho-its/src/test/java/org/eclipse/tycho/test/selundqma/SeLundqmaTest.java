/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.selundqma;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SeLundqmaTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		final boolean ignoreLocallyInstalledArtifacts = false;

		// build01
		Verifier v01 = getVerifier("selundqma", true, ignoreLocallyInstalledArtifacts);
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();

		// build02
		Verifier v02 = getVerifier("selundqma/clients", true, ignoreLocallyInstalledArtifacts);
		v02.executeGoal("install");
		v02.verifyErrorFreeLog();
	}

}
