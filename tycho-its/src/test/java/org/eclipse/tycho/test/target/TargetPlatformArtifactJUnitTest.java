package org.eclipse.tycho.test.target;

import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

// See PR https://github.com/eclipse-tycho/tycho/pull/1773
public class TargetPlatformArtifactJUnitTest extends AbstractTychoIntegrationTest {
	@Test
	@Ignore("No longer works, needs update to more recent examples")
	public void testTargetPlatformForJUnit5() throws Exception {
		Verifier verifier = getVerifier("target.artifact.junit", false, true);
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}
}
