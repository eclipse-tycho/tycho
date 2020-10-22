/*******************************************************************************
 * Copyright (c) 2013, 2018 Red Hat Inc. and others
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

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class P2InstalledTestRuntimeTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProvisionAppAndRunTest() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
        options.add("-PprovisionProduct");
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testRunTestOnProvisionedApp() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
        options.add("-PuseProvisionedProduct");
        options.add("-DproductClassifier=" + getProductClassifier());
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testDifferentHarnessVersions() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
        // Use different TP for test bundle and product under test
        options.add("-Dother.p2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
        options.add("-PuseProvisionedProduct");
        options.add("-DproductClassifier=" + getProductClassifier());
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    private static String getProductClassifier() {
        TargetEnvironment currentEnv = TargetEnvironment.getRunningEnvironment();
        return currentEnv.getOs() + "." + currentEnv.getWs() + "." + currentEnv.getArch();
    }
}
