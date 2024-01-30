package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.junit.Test;

public class P2DirectorPluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDirectorStandalone() throws Exception {
		Verifier verifier = getVerifier("tycho-p2-director-plugin/director-goal-standalone", true, true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

}
