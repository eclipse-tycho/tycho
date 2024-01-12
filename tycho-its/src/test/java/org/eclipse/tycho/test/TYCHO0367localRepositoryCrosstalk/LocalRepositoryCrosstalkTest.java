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

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class LocalRepositoryCrosstalkTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		// run bundle 2 test first with latest eclipse
		Verifier v01 = getVerifier("/TYCHO0367localRepositoryCrosstalk/bundle02");
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();

		// now run bundle1 test, it should not "see" artifacts in local repo from newer
		// update site
		Verifier v02 = getVerifier("/TYCHO0367localRepositoryCrosstalk/bundle01", false);
		v02.addCliOption("-Dp2.repo=https:////download.eclipse.org/releases/photon/");
		v02.executeGoal("install");
		v02.verifyErrorFreeLog();
	}

}
