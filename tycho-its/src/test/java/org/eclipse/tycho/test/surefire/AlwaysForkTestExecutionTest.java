package org.eclipse.tycho.test.surefire;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class AlwaysForkTestExecutionTest extends AbstractTychoIntegrationTest {
    @Test
    public void testForkExecution() throws Exception {
        final String projectName = "/surefire.junit47/fork";
        Verifier verifier = getVerifier(projectName);
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        File targetDir = new File(getBasedir(projectName), "target");
        StringBuffer[] pids = new StringBuffer[2];
        for (int i = 1; i <= pids.length; i++) {
            File pidFile = new File(targetDir, "test" + i + "-pid");
            StringBuffer pid = readFileToString(pidFile);
            pids[i - 1] = pid;
        }
        Assert.assertFalse(pids[0].equals(pids[1]));
    }
}
