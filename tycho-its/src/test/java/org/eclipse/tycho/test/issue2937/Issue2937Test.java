package org.eclipse.tycho.test.issue2937;

import java.util.List;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Issue2937Test extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("issue2937");

		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}
}
