package org.eclipse.tycho.test.compiler;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

public class Fragments2Test extends AbstractTychoIntegrationTest {

	/**
	 * Currently fails: unlike a packaged/target-platform fragment (see {@link FragmentsTest}),
	 * a fragment that is itself a <b>reactor project</b> is not added to the compile classpath
	 * of a bundle that requires its host. See https://github.com/eclipse-tycho/tycho/issues/626
	 * for the original (working) case this is modeled after.
	 */
	@Test
	public void testFragment() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments2", false);
		assertThrows(VerificationException.class, () -> verifier.executeGoal("compile"));
		verifier.verifyTextInLog("HostClassInFragment cannot be resolved to a type");
	}

	/**
	 * Workaround: the host bundle declares an explicit <code>p2.inf</code> requirement on the
	 * fragment (as org.eclipse.swt does for its platform fragments), forcing the fragment to be
	 * resolved like a normal dependency instead of going through the (currently broken) implicit
	 * "dependency fragment" resolution. To avoid introducing a reactor build cycle (host requires
	 * fragment, fragment requires host via Fragment-Host), the requirement is disabled via a
	 * profile property while the host itself is being built - again mirroring org.eclipse.swt.
	 */
	@Test
	public void testFragmentP2Inf() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments2.p2inf", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Workaround: the consuming bundle declares an explicit <code>jars.extra.classpath</code>
	 * entry in build.properties pointing at the fragment (<code>platform:/fragment/...</code>),
	 * which makes the fragment a normal resolved dependency of that bundle.
	 */
	@Test
	public void testFragmentExtraClasspath() throws Exception {
		Verifier verifier = getVerifier("compiler.fragments2.extraclasspath", false);
		verifier.executeGoal("compile");
		verifier.verifyErrorFreeLog();
	}
}
