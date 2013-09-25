/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat JBoss) - Test
 *******************************************************************************/
package org.eclipse.tycho.test.surefire.provisionedApplication;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class ProvisioningTest extends AbstractTychoIntegrationTest {

    @Test
    public void testProvisionAppAndRunTest() throws Exception {
        Verifier verifier = getVerifier("surefire.provisionedApplication", false);
        List options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
        options.add("-PprovisionProduct");
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testProvisionAppWithDifferentTPAndRunTest() throws Exception {
        // Builds a product with 3.4.2
        Verifier verifier = getVerifier("surefire.provisionedApplication", false);
        List options = verifier.getCliOptions();
        String repoParameter = "-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString();
        options.add(repoParameter);
        verifier.executeGoals(asList("clean", "install"));
        verifier.verifyErrorFreeLog();
        options.remove(repoParameter);

        // Runs test against 3.4.2 application using 3.5.2 as TP
        TargetEnvironment currentEnv = TargetEnvironment.getRunningEnvironment();
        File applicationPath = new File(verifier.getBasedir(), "example-product/target/products/example-product"
                + currentEnv.getOs() + "/" + currentEnv.getWs() + "/" + currentEnv.getArch());
        repoParameter = "-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352;
        options.add(repoParameter);
        options.add("-DproductClassifier=" + getProductClassifier());
        options.add("-f");
        options.add(verifier.getBasedir() + "/testProvisionedApplication/pom.xml");
        verifier.executeGoals(asList("clean", "integration-test"));
        verifier.verifyErrorFreeLog();

    }

    @Test
    public void testRunTestOnProvisionedApp() throws Exception {
        Verifier verifier = getVerifier("surefire.provisionedApplication", false);
        List options = verifier.getCliOptions();
        options.add("-Dp2.repo.url=" + ResourceUtil.P2Repositories.ECLIPSE_352.toString());
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
