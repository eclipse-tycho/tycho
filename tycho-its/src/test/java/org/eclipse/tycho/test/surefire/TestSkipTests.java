package org.eclipse.tycho.test.surefire;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class TestSkipTests extends AbstractTychoIntegrationTest {

    // The test "surefire.skipTests" is a test, that does fail.
    // With that test, this mojo test can test if the tests had been executed or not

    @Test
    public void testWithSkipTestsSetToTrue() throws Exception {
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("skipTests", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testWithoutSettingSkipTests() throws Exception {
        // default must be to execute the tests so we except that building
        // the surefire.skipTests project to fail because of failing tests.
        Verifier verifier = getVerifier("surefire.skipTests");
        try {
            verifier.executeGoal("integration-test");
            Assert.fail();
        } catch (VerificationException e) {
            // expected
        }
        verifier.verifyTextInLog("BUILD FAILURE");
    }

    @Test
    public void testWithSettingMavenTestSkipSetToTrue() throws Exception {
        // if the tests are skipped, build must succeed.
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testWithBothSettingsSetWithDifferentValuesSkipTestsTrue() throws Exception {
        // if both parameters are set with different values, skipTests must win
        // and so the build must success, because the failing test would not be executed.
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "false");
        verifier.getSystemProperties().setProperty("skipTests", "true");
        verifier.executeGoal("integration-test");
        verifier.verifyTextInLog("Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority!");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testWithBothSettingsSetWithDifferentValuesSkipTestsFalse() throws Exception {
        // both parameters are set, skipTests is set to false, so the failing test must be
        // executed and the build must fail.
        Verifier verifier = getVerifier("surefire.skipTests");
        verifier.getSystemProperties().setProperty("maven.test.skip", "true");
        verifier.getSystemProperties().setProperty("skipTests", "false");
        try {
            verifier.executeGoal("integration-test");
            Assert.fail();
        } catch (VerificationException e) {
            // expected
        }
        verifier.verifyTextInLog("Both parameter 'skipTests' and 'maven.test.skip' are set, 'skipTests' has a higher priority!");
        verifier.verifyTextInLog("BUILD FAILURE");
    }

}
