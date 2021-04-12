package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.assertTestMethodWasSuccessfullyExecuted;

import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class JUnit5TempDirTest extends AbstractTychoIntegrationTest {

    @Test
    public void testJUnit5Runner() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit5tempdir/bundle.test", false);
        Properties props = verifier.getSystemProperties();
        props.setProperty("2019-09-repo", P2Repositories.ECLIPSE_LATEST.toString());
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
        String projectBasedir = verifier.getBasedir();
        assertTestMethodWasSuccessfullyExecuted(projectBasedir, "bundle.test.JUnit5TempDirTest", "tempDirTest{File}");
    }

}
