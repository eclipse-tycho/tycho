/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.assertNumberOfSuccessfulTests;
import static org.eclipse.tycho.test.util.SurefireUtil.assertTestMethodWasSuccessfullyExecuted;

import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class JUnit5Test extends AbstractTychoIntegrationTest {

    @Test
    public void testJUnit5Runner() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit5/bundle.test", false);
        Properties props = verifier.getSystemProperties();
        props.setProperty("oxygen-repo", P2Repositories.ECLIPSE_OXYGEN.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        String projectBasedir = verifier.getBasedir();
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit4Test", "testWithJUnit4");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
                "myFirstJUnit5Test{TestInfo}");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
                "parameterizedJUnit5Test{String}[1]");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test",
                "parameterizedJUnit5Test{String}[2]");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "repeatedJUnit5Test[1]");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "repeatedJUnit5Test[2]");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5Test", "repeatedJUnit5Test[3]");
        // make sure test tagged as 'slow' was skipped
        assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit5Test", 6);
    }

    @Test
    public void testJUnit54Runner() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit54/bundle.test", false);
        Properties props = verifier.getSystemProperties();
        props.setProperty("repo-2019-03", P2Repositories.ECLIPSE_LATEST.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        String projectBasedir = verifier.getBasedir();
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit4Test", "testWithJUnit4");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit54Test",
                "myFirstJUnit54Test{TestInfo}");
        // make sure test tagged as 'slow' was skipped
        assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit54Test", 1);
    }

}
