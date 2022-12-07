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
package org.eclipse.tycho.test.multiplatform;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class MultiplatformReactorTest extends AbstractTychoIntegrationTest {

	@Test
	public void testMultiplatformReactorBuild() throws Exception {
		Verifier verifier = getVerifier("multiPlatform.reactor", false);
		verifier.addCliArgument("-Dtarget-platform=" + P2Repositories.ECLIPSE_LATEST.toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		// assert product got proper platform fragments
		File productTarget = new File(verifier.getBasedir(), "product/target");
		assertFileExists(productTarget, "products/mpr.product/linux/gtk/x86_64/plugins/mpr.fragment.linux_0.0.1.*.jar");
		assertFileExists(productTarget,
				"products/mpr.product/win32/win32/x86_64/plugins/mpr.fragment.windows_0.0.1.*.jar");

		// assert site got all platform fragments
		File siteproductTarget = new File(verifier.getBasedir(), "site/target");
		assertFileExists(siteproductTarget, "repository/plugins/mpr.fragment.linux_0.0.1.*.jar");
		assertFileExists(siteproductTarget, "repository/plugins/mpr.fragment.windows_0.0.1.*.jar");
	}
}
