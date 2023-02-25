package org.eclipse.tycho.test.issue697;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

public class Issue697Test extends AbstractTychoIntegrationTest {

	@Test
	@Ignore
	public void test() throws Exception {
		Verifier verifier = getVerifier("issue697");

		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}
}
