package org.eclipse.tycho.test.issue1512;

import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Issue1512Test extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifierRepo = getVerifier("issue1512");
		verifierRepo.executeGoals(List.of("clean", "package"));
		verifierRepo.verifyErrorFreeLog();

		Verifier verifierBundle = getVerifier("issue1512/bundle");
		verifierBundle.executeGoals(List.of("clean", "compile"));
		verifierBundle.verifyErrorFreeLog();
	}

}
