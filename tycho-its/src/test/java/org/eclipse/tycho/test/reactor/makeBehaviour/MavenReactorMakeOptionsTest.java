package org.eclipse.tycho.test.reactor.makeBehaviour;

import static org.junit.Assert.fail;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test Maven reactor make behaviours
 *
 * Test project dependencies:
 *
 * <pre>
 * feature2 -> feature1,bundle2
 * feature1 -> bundle1
 * </pre>
 *
 */
public class MavenReactorMakeOptionsTest extends AbstractTychoIntegrationTest {

	private Verifier verifier;

	@Before
	public void setUp() throws Exception {
		verifier = getVerifier("reactor.makeBehaviour");
		verifier.executeGoal("clean");
	}

	@Test
	public void testCompleteBuild() throws Exception {
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testAlsoMake() throws Exception {
		// REACTOR_MAKE_UPSTREAM
		verifier.addCliOption("-am");
		verifier.addCliOption("-pl feature1");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testAlsoMakeDependents() throws Exception {
		// REACTOR_MAKE_DOWNSTREAM
		verifier.addCliOption("-amd");
		verifier.addCliOption("-pl bundle1,bundle2");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testBoth() throws Exception {
		// REACTOR_MAKE_BOTH
		verifier.addCliOption("-am");
		verifier.addCliOption("-amd");
		verifier.addCliOption("-pl feature1,bundle2");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testSingleProjectNoOptionFails() throws Exception {
		try {
			verifier.addCliOption("-pl feature1");
			verifier.executeGoal("verify");
			fail("Build should fail due to missing reactor dependency");
		} catch (VerificationException e) {
			verifier.verifyTextInLog(
					"Missing requirement: feature1.feature.group 1.0.0.qualifier requires 'org.eclipse.equinox.p2.iu; bundle1 0.0.0' but it could not be found");
		}
	}

	@Test
	public void testDownstreamFailsIfMissingDownstreamDependency() throws Exception {
		// Downstream brings in feature2 but this requires bundle2 which is unrelated
		// to specified projects
		try {
			verifier.addCliOption("-amd");
			verifier.addCliOption("-pl bundle1,feature1");
			verifier.executeGoal("verify");
			fail("Build should fail due to missing reactor dependency");
		} catch (VerificationException e) {
			verifier.verifyTextInLog(
					"Missing requirement: feature2.feature.group 1.0.0.qualifier requires 'org.eclipse.equinox.p2.iu; bundle2 0.0.0' but it could not be found");
		}
	}

}
