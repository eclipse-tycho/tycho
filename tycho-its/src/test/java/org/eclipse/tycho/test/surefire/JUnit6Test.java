/*******************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.assertNumberOfSuccessfulTests;
import static org.eclipse.tycho.test.util.SurefireUtil.assertTestMethodWasSuccessfullyExecuted;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class JUnit6Test extends AbstractTychoIntegrationTest {

    /**
     * Tests that the JUnit 6 provider can be explicitly selected using providerHint.
     * Since JUnit 6 doesn't exist yet, this test uses JUnit 5.x libraries with the
     * junit6 provider hint to verify that the provider infrastructure is in place.
     * 
     * @throws Exception
     */
    @Test
    public void testJUnit6ProviderHint() throws Exception {
        final Verifier verifier = getVerifier("/tycho-surefire-plugin/junit6/basic");
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        final String projectBasedir = verifier.getBasedir();
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test", "My 1st JUnit 6 test!");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test",
                "parameterizedJUnit6Test(String)[1] one");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test",
                "parameterizedJUnit6Test(String)[2] two");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test",
                "repeatedJUnit6Test() repetition 1 of 3");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test",
                "repeatedJUnit6Test() repetition 2 of 3");
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit6Test",
                "repeatedJUnit6Test() repetition 3 of 3");
        // make sure test tagged as 'slow' was skipped
        assertNumberOfSuccessfulTests(projectBasedir, "bundle.test.JUnit6Test", 6);
    }

}
