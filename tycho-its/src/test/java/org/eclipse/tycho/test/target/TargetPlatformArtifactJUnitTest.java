package org.eclipse.tycho.test.target;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import java.util.List;

// See PR https://github.com/eclipse-tycho/tycho/pull/1773
public class TargetPlatformArtifactJUnitTest extends AbstractTychoIntegrationTest {
    @Test
    public void testTargetPlatformForJUnit5() throws Exception {
        Verifier verifier = getVerifier("target.artifact.junit", false, true);
        verifier.executeGoals(List.of("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }
}
