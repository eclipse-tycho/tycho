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
	 * Test that removing a source bundle or source feature from a feature only requires a minor
	 * version bump.
	 * 
	 * This test verifies the fix for the issue where FeatureBaselineComparator was
	 * treating removal of source artifacts (requirements ending with ".source" for bundles or
	 * ".source.feature.group" for features) as a major version change. Source artifacts are
	 * deprecated and should only trigger a minor version bump when removed.
	 */
	@Test
	public void testRemoveSourceBundle() throws Exception {
		// Build feature that removes the source bundle with only minor version bump
		// (1.0.0 -> 1.1.0)
		// This should pass with the fix
		Verifier verifier = getBaselineProject("feature-remove-source");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		// This should succeed because removing .source bundle only requires minor
		// version bump
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Test that removing a source feature from a feature only requires a minor
	 * version bump.
	 * 
	 * This test verifies that FeatureBaselineComparator correctly handles removal of source
	 * features which have requirements ending with ".source.feature.group". Like source bundles,
	 * source features are deprecated and should only trigger a minor version bump when removed.
	 */
	@Test
	public void testRemoveSourceFeature() throws Exception {
		// Build feature that removes the source feature with only minor version bump
		// (1.0.0 -> 1.1.0)
		// This should pass with the fix
		Verifier verifier = getBaselineProject("feature-remove-source-feature");
		verifier.addCliOption("-Dbaseline-url=" + baselineRepo.toURI());

		// This should succeed because removing .source.feature.group only requires minor
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

	/**
	 * Test the check-dependencies goal with applySuggestions=true. Verifies that
	 * version ranges are correctly generated for various Import-Package and
	 * Require-Bundle scenarios:
	 * <ul>
	 * <li>Import-Package with too-low version range gets corrected</li>
	 * <li>Import-Package without version gets a proper range</li>
	 * <li>Require-Bundle with range gets lower bound bumped without qualifier</li>
	 * <li>Require-Bundle without upper bound gets [version,nextMajor) not
	 * [version.qualifier,null)</li>
	 * </ul>
	 */
	@Test
	public void testDependencyCheck() throws Exception {
		Verifier verifier = getVerifier("baselinePlugin", true, true);
		verifier.getCliOptions().remove("-X");
		verifier.addCliOption("-f");
		verifier.addCliOption("check-dependencies/pom.xml");
		verifier.executeGoals(List.of("clean", "verify"));

		File basedir = new File(verifier.getBasedir());
		File checkDepsDir = new File(basedir, "check-dependencies");

		// Import-Package with explicit range [1.5.0,2.0.0) should be updated
		// to raise lower bound because Bundle.adapt(Class) was added in 1.6.0
		ManifestAssertions.of(manifestOf(checkDepsDir, "import-package-low-range"))
				.assertPackageLowerBound("org.osgi.framework", "1.6.0",
						"Lower bound must be 1.6.0 because Bundle.adapt was added in 1.6.0")
				.assertPackageUpperBound("org.osgi.framework", "2.0.0",
						"Upper bound should be preserved from original range");

		// Import-Package without version should get a new range [1.6.0,2)
		ManifestAssertions.of(manifestOf(checkDepsDir, "import-package-unversioned"))
				.assertPackageLowerBound("org.osgi.framework", "1.6.0",
						"Lower bound must be 1.6.0 for Bundle.adapt method")
				.assertPackageUpperBound("org.osgi.framework", "2.0.0",
						"Upper bound should be next major version");

		// Require-Bundle with range [3.4.0,4.0.0) should have lower bound updated
		// without qualifier
		ManifestAssertions.of(manifestOf(checkDepsDir, "require-bundle-with-range"))
				.assertBundleLowerBound("org.eclipse.equinox.common", "3.5.0",
						"Lower bound must be 3.5.0 because URIUtil.append was added in 3.5.0")
				.assertBundleUpperBound("org.eclipse.equinox.common", "4.0.0",
						"Upper bound should be preserved from original range");

		// Require-Bundle with simple version "3.4.0" (no upper bound) should become
		// [3.5.0,4) not [3.5.0.qualifier,null)
		ManifestAssertions.of(manifestOf(checkDepsDir, "require-bundle-no-upper-bound"))
				.assertBundleLowerBound("org.eclipse.equinox.common", "3.5.0",
						"Lower bound must be 3.5.0 because URIUtil.append was added in 3.5.0")
				.assertBundleUpperBound("org.eclipse.equinox.common", "4.0.0",
						"Upper bound should be next major version, not 'null'");

		// Require-Bundle with split package: org.eclipse.equinox.common and
		// org.eclipse.equinox.registry both export org.eclipse.core.runtime.
		// The checker must not blame common for types from registry.
		ManifestAssertions.of(manifestOf(checkDepsDir, "require-bundle-split-package"))
				.assertBundleLowerBound("org.eclipse.equinox.common", "3.5.0",
						"Lower bound for common must reflect URIUtil.append, not registry types like IConfigurationElement")
				.assertBundleUpperBound("org.eclipse.equinox.common", "4.0.0",
						"Upper bound for common should be preserved from original range");

		// Require-Bundle with re-export: org.eclipse.core.runtime re-exports
		// org.eclipse.equinox.common (visibility:=reexport). CoreException lives in
		// org.eclipse.equinox.common, not in org.eclipse.core.runtime itself.
		// The checker must not attribute CoreException to org.eclipse.core.runtime.
		ManifestAssertions.of(manifestOf(checkDepsDir, "require-bundle-reexport"))
				.assertBundleLowerBound("org.eclipse.core.runtime", "3.34.0",
						"Lower bound must stay unchanged because CoreException is from re-exported org.eclipse.equinox.common")
				.assertBundleUpperBound("org.eclipse.core.runtime", "4.0.0",
						"Upper bound should be preserved from original range");

		// Require-Bundle with range [3.20.0,4) where the lower bound is already
		// correct. The suggested range [3.20.0,4.0.0) is semantically equivalent,
		// so the manifest must remain untouched (no cosmetic reformatting).
		ManifestAssertions.of(manifestOf(checkDepsDir, "require-bundle-correct-range"))
				.assertBundleRawVersion("org.eclipse.equinox.common", "[3.20.0,4)",
						"Version range must stay as [3.20.0,4) and not be reformatted to [3.20.0,4.0.0)");
	}

	private static File manifestOf(File projectDir, String module) {
		return new File(projectDir, module + "/META-INF/MANIFEST.MF");
	}
}
