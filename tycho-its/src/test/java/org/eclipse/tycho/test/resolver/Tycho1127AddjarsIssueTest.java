package org.eclipse.tycho.test.resolver;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho1127AddjarsIssueTest extends AbstractTychoIntegrationTest {

	@Test
	public void testTycho1127AddjarsIssue() throws Exception {
		Verifier verifier = getVerifier("resolver.tycho1127_addjars_issue", false, false);
		verifier.addCliArgument("-Dtycho.resolver.classic=false");
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
	}
}