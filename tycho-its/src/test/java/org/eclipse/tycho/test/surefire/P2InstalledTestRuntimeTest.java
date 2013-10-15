/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat JBoss) - Test
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class P2InstalledTestRuntimeTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProvisionAppAndRunTest() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
        options.add("-PprovisionProduct");
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testRunTestOnProvisionedApp() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
        options.add("-PuseProvisionedProduct");
        options.add("-DproductClassifier=" + getProductClassifier());
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testDifferentHarnessVersions() throws Exception {
        Verifier verifier = getVerifier("surefire.p2InstalledRuntime", false);
        List<String> options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
        // Use different TP for test bundle and product under test
        options.add("-Dother.p2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
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
