package org.eclipse.tycho.test.buildextension;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomlessTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBnd() throws Exception {
		Verifier verifier = getVerifier("pomless", false, true);
		verifier.addCliOption("-pl");
		verifier.addCliOption("bnd");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bnd/target/classes/module-info.class");
		assertTrue("module-info.class is not generated!", file.isFile());
	}
}
