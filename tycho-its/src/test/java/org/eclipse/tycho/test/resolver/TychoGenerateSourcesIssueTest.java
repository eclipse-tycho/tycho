package org.eclipse.tycho.test.resolver;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TychoGenerateSourcesIssueTest extends AbstractTychoIntegrationTest {
    @Test
    public void testTychoGenerateSourcesIssue() throws Exception {
        Verifier verifier = getVerifier("resolver.generateSources", false, false);
        verifier.addCliOption("-Dtycho.resolver.classic=false");
        verifier.executeGoals(List.of("clean", "generate-sources"));
        verifier.verifyErrorFreeLog();
    }
}
