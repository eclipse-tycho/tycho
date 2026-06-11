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
package org.eclipse.tycho.test.TYCHO246rcpSourceBundles;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO246rcpSourceBundlesTest extends AbstractTychoIntegrationTest {
	@Test
	public void testMultiplatformReactorBuild() throws Exception {
		Verifier verifier = getVerifier("/TYCHO246rcpSourceBundles");
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		File productTarget = new File(verifier.getBasedir(), "product/target");
		assertFileExists(productTarget, "repository/plugins/org.eclipse.osgi_*.jar");
		assertFileExists(productTarget, "repository/plugins/org.eclipse.osgi.source_*.jar");
	}

}
