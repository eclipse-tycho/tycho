/*******************************************************************************
 * Copyright (c) 2013, 2021 Red Hat Inc. and others
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat JBoss) - Test
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class P2InstalledTestRuntimeTest extends AbstractTychoIntegrationTest {

	@Test
	public void testProvisionAppAndRunTest() throws Exception {
		Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
		verifier.addCliArgument("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.addCliArgument("-PprovisionProduct");
		verifier.executeGoals(List.of("clean", "integration-test"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testRunTestOnProvisionedApp() throws Exception {
		Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
		verifier.addCliArgument("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.addCliArgument("-PuseProvisionedProduct");
		verifier.addCliArgument("-DproductClassifier=" + getProductClassifier());
		verifier.executeGoals(List.of("clean", "integration-test"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testDifferentHarnessVersions() throws Exception {
		Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
		verifier.addCliArgument("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
		// Use different TP for test bundle and product under test
		verifier.addCliArgument("-Dother.p2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
		verifier.addCliArgument("-PuseProvisionedProduct");
		verifier.addCliArgument("-DproductClassifier=" + getProductClassifier());
		verifier.executeGoals(List.of("clean", "integration-test"));
		verifier.verifyErrorFreeLog();
	}

	private static String getProductClassifier() {
		TargetEnvironment currentEnv = TargetEnvironment.getRunningEnvironment();
		return String.join(".", currentEnv.getOs(), currentEnv.getWs(), currentEnv.getArch());
	}
}
