package org.eclipse.tycho.test.toolchains;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JavaToolchainInSurefireTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/toolchains");
        File toolchains = new File(verifier.getBasedir() + "/toolchains.xml");
        verifier.getCliOptions().add("--toolchains " + toolchains.getCanonicalPath());
        verifier.executeGoal("integration-test");
        verifier.verifyTextInLog("Toolchain in tycho-surefire-plugin: JDK[fake-jdk-home]");
    }
}
