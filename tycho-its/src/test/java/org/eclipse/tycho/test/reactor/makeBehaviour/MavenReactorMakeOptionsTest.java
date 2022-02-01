/*******************************************************************************
 * Copyright (c) 2022 Joe Shannon and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Joe Shannon - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.reactor.makeBehaviour;

import static org.junit.Assert.fail;

import java.util.List;

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
 *            bundle1a <-- bundle1b
 *            |
 *            |
 *  bundle1  <-- feature1 <-- feature2
 *                            |
 *                 bundle2 <--|
 * </pre>
 *
 */
public class MavenReactorMakeOptionsTest extends AbstractTychoIntegrationTest {

	private Verifier verifier;

	@Before
	public void setUp() throws Exception {
		verifier = getVerifier("reactor.makeBehaviour");
		verifier.addCliOption("-T1C");
	}

	@Test
	public void testCompleteBuild() throws Exception {
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1a/target/bundle1a-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1b/target/bundle1b-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testAlsoMake() throws Exception {
		// REACTOR_MAKE_UPSTREAM
		verifier.addCliOption("-am");
		verifier.addCliOption("-pl feature1");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("bundle1a/target/bundle1a-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("bundle1b/target/bundle1b-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testAlsoMakeWithIndirectDependencies() throws Exception {
		// REACTOR_MAKE_UPSTREAM
		verifier.addCliOption("-am");
		verifier.addCliOption("-pl bundle1b");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1a/target/bundle1a-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1b/target/bundle1b-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("bundle2/target/bundle2-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("feature1/target/feature1-1.0.0-SNAPSHOT.jar");
		verifier.assertFileNotPresent("feature2/target/feature2-1.0.0-SNAPSHOT.jar");
	}

	@Test
	public void testAlsoMakeDependentsNeedsToPickUpDependenciesOfDependents() throws Exception {
		// REACTOR_MAKE_DOWNSTREAM
		verifier.addCliOption("-amd");
		verifier.addCliOption("-pl bundle1");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
		verifier.assertFilePresent("bundle1/target/bundle1-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1a/target/bundle1a-1.0.0-SNAPSHOT.jar");
		verifier.assertFilePresent("bundle1b/target/bundle1b-1.0.0-SNAPSHOT.jar");
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
		verifier.executeGoals(List.of("clean", "verify"));
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
			verifier.executeGoals(List.of("clean", "verify"));
			fail("Build should fail due to missing reactor dependency");
		} catch (VerificationException e) {
			verifier.verifyTextInLog(
					"Missing requirement: feature1.feature.group 1.0.0.qualifier requires 'org.eclipse.equinox.p2.iu; bundle1 0.0.0' but it could not be found");
		}
	}

}
