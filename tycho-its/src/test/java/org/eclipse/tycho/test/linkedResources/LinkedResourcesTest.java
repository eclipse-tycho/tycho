package org.eclipse.tycho.test.linkedResources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class LinkedResourcesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLinkedSettingsFolder() throws Exception {
		Verifier verifier = getVerifier("linked-resources", false);
		verifier.executeGoals(List.of("verify"));

		verifyNoUseProjectSettingsWarning(verifier);
	}

	private void verifyNoUseProjectSettingsWarning(Verifier verifier) throws VerificationException {
		List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);

		String warningMsg = "[WARNING] Parameter 'useProjectSettings' is set to true, but preferences file";

		assertThat(lines, not(hasItem(containsString(warningMsg))));
	}
}
