package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class Fragments2Test extends AbstractTychoIntegrationTest {
	@Test
	public void testFragment() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments2", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}
}
