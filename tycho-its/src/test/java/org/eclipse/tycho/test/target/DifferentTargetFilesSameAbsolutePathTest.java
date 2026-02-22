package org.eclipse.tycho.test.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DifferentTargetFilesSameAbsolutePathTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("sameAbsoluteTarget");

		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		// 2 modules with relative path to target, but identical absolute target file
		// path
		assertEquals(1, Files.lines(Path.of(verifier.getBasedir(), verifier.getLogFileName()))
				.filter(line -> line.contains("Resolving target definition file")).count());
	}
}
