package org.eclipse.tycho.test.target;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import java.util.List;

// See issue https://github.com/eclipse-tycho/tycho/issues/4653
public class TargetPlatformEagerResolverTest extends AbstractTychoIntegrationTest {
    @Test
    public void testTargetPlatformForJUnit5() throws Exception {
        Verifier verifier = getVerifier("target.eagerResolver", false, true);
        verifier.executeGoals(List.of("clean", "verify"));
        verifier.verifyErrorFreeLog();
    }
}
