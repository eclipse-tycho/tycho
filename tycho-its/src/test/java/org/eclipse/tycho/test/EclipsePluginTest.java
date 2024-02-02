package org.eclipse.tycho.test;

import org.apache.maven.it.Verifier;
import org.junit.Test;

public class EclipsePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDirectorStandalone() throws Exception {
		Verifier verifier = getVerifier("tycho-eclipse-plugin/run-ant", true, true);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		verifier.verifyFilePresent("target/.run.ok");
	}

}