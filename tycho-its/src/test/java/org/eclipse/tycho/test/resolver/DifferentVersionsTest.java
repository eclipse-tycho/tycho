package org.eclipse.tycho.test.resolver;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DifferentVersionsTest extends AbstractTychoIntegrationTest {

    @Test
    public void testBundleNativeCode() throws Exception {
        Verifier verifier = getVerifier("resolver.differentVersions");
        verifier.executeGoal("compile");
        verifier.verifyErrorFreeLog();
    }
}
