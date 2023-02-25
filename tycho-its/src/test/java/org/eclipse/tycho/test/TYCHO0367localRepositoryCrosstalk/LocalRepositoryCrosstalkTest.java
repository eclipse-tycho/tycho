/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO0367localRepositoryCrosstalk;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class LocalRepositoryCrosstalkTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		// run e352 test first
		Verifier v01 = getVerifier("/TYCHO0367localRepositoryCrosstalk/bundle02", false);
		v01.addCliArgument("-Dp2.repo=" + P2Repositories.ECLIPSE_LATEST.toString());
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();

		// now run e342 test, it should not "see" e352 artifacts in local repo
		Verifier v02 = getVerifier("/TYCHO0367localRepositoryCrosstalk/bundle01", false);
		v02.addCliArgument("-Dp2.repo=" + P2Repositories.ECLIPSE_OXYGEN.toString());
		v02.executeGoal("install");
		v02.verifyErrorFreeLog();
	}

}
