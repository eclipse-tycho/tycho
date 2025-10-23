/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the tycho-baseline-plugin with features containing source features.
 */
public class BaselinePluginTest extends AbstractTychoIntegrationTest {

	private static File baselineRepo = null;

	@Before
	public void buildBaselineRepository() throws Exception {
		if (baselineRepo == null) {
			File repoLocation = buildBaseRepo();
			baselineRepo = new File("target/projects", getClass().getSimpleName() + "/baselineRepo").getAbsoluteFile();
			FileUtils.copyDirectoryStructure(repoLocation, baselineRepo);
		}
	}

	/**
	 * Test that removing a source feature from a feature only requires a minor
	 * version bump.
	 * 
	 * This test verifies the fix for the issue where FeatureBaselineComparator was
	 * treating removal of source features (requirements ending with ".source") as a
	 * major version change. Source features are deprecated and should only trigger
	 * a minor version bump.
	 */
	@Test
	public void testRemoveSourceFeature() throws Exception {
		// Build feature that removes the source feature with only minor version bump
		// (1.0.0 -> 1.1.0)
		// This should pass with the fix
		Verifier verifier = getBaselineProject("feature-remove-source");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		// This should succeed because removing .source feature only requires minor
		// version bump
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Test that removing a regular (non-source) bundle requires a major version
	 * bump.
	 */
	@Test
	public void testRemoveRegularBundle() throws Exception {
		// Removing a regular bundle should require major version bump (1.0.0 -> 2.0.0)
		Verifier verifier = getBaselineProject("feature-remove-bundle");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		// This should succeed because we bumped to major version
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Test that adding a method to a class causes an API break in derived classes
	 * through inheritance.
	 * 
	 * This test verifies that when class A adds a new method m(), class B which
	 * extends A now exposes this method too. If package q (containing B) doesn't
	 * bump its version, this should be flagged as an API break.
	 */
	@Test
	public void testApiBreakWithInheritance() throws Exception {
		// Build bundle where class A adds method m() but package q version is not
		// bumped
		// This should fail because B extends A and now exposes the new method
		Verifier verifier = getBaselineProject("bundle-with-inheritance");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		assertThrows(VerificationException.class, () -> {
			verifier.executeGoals(List.of("clean", "verify"));
			verifier.verifyErrorFreeLog();
		});
		verifier.verifyTextInLog("Baseline problems found");
	}

	private File buildBaseRepo() throws Exception, VerificationException {
		Verifier verifier = getBaselineProject("base-repo");
		verifier.addCliOption("-Dtycho.baseline.skip=true");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File repoBase = new File(verifier.getBasedir(), "base-repo/site/target/repository");
		assertTrue("base repository was not created at " + repoBase.getAbsolutePath(), repoBase.isDirectory());
		assertTrue("content.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "content.jar").isFile());
		assertTrue("artifacts.jar was not created at " + repoBase.getAbsolutePath(),
				new File(repoBase, "artifacts.jar").isFile());
		return repoBase;
	}

	private Verifier getBaselineProject(String project) throws Exception {
		Verifier verifier = getVerifier("baselinePlugin", false, true);
		verifier.addCliOption("-f");
		verifier.addCliOption(project + "/pom.xml");
		return verifier;
	}
}
