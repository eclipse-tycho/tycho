package org.eclipse.tycho.test.pomDependencyConsider;

import static java.util.Arrays.asList;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PackagingPomDependencyWithTimestampProviderTest extends AbstractTychoIntegrationTest {

	@Test
	public void testPackage() throws Exception {
		// project with pomDependency=consider, and jgit build timestamp provider and a
		// feature referencing a non-Tycho artifact
		Verifier verifier = getVerifier("pomDependencyConsider.buildtimestamp.jgit", true);
		verifier.executeGoals(asList("clean", "package"));
		// Test should not fail, but will fail with:
		// "Could not resolve plugin com.google.guava_0.0.0"
	}
}
