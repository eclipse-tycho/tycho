package org.eclipse.tycho.test.resolver;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JustJJRETest extends AbstractTychoIntegrationTest {

	@Test
	public void testProductWithJustJJREdifferentToRunningJVM() throws Exception {
		Verifier verifier = getVerifier("resolver.justjJRE");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

}
