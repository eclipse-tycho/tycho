package org.eclipse.tycho.test.resolver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho1127AddjarsIssueTest extends AbstractTychoIntegrationTest {

	@Test
    public void testTycho1127AddjarsIssue() throws Exception {
        Verifier verifier = getVerifier("resolver.tycho1127_addjars_issue", false, false);
        verifier.addCliOption("-Dtycho.resolver.classic=true");
        verifier.executeGoals(List.of("clean","install"));
        verifier.verifyErrorFreeLog();
    }
}