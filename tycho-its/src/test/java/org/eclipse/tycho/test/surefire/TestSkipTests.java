/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TestSkipTests extends AbstractTychoIntegrationTest {

    @Test
    public void testWithSkipTestsSetToTrue() throws Exception {
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("skipTests", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        try {
            verifier.verifyTextInLog("T E S T S");
            Assert.fail("skipTests=true must not execute any tests!");
        } catch (VerificationException e) {
            // expected
        }
    }

    @Test
    public void testWithoutSettingSkipTests() throws Exception {
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("T E S T S");
    }

    @Test
    public void testWithSettingMavenTestSkipSetToTrue() throws Exception {
        // only maven.test.skip is set to true, so tests must not be executed
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        try {
            verifier.verifyTextInLog("T E S T S");
            Assert.fail("maven.test.skip=true must not execute any tests!");
        } catch (VerificationException e) {
            // expected
        }
    }

    @Test
    public void testWithBothSettingsSetWithDifferentValuesSkipTestsTrue() throws Exception {
        // if both parameters are set with different values, skipTests must win and 
        // no test must be executed
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "false");
        verifier.getSystemProperties().setProperty("skipTests", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyTextInLog("Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority!");
        verifier.verifyErrorFreeLog();
        try {
            verifier.verifyTextInLog("T E S T S");
            Assert.fail("skipTests=true must not execute any tests!");
        } catch (VerificationException e) {
            // expected
        }
    }

    @Test
    public void testWithBothSettingsSetWithDifferentValuesSkipTestsFalse() throws Exception {
        // both parameters are set, skipTests is set to false, so the test must be executed
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "true");
        verifier.getSystemProperties().setProperty("skipTests", "false");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority!");
        verifier.verifyTextInLog("T E S T S");
    }

}
