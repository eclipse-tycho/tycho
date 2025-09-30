/*******************************************************************************
 * Copyright (c) 2012, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.buildtimestamp;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/**
 * Tests for tycho-buildtimestamp-jgit functionality.
 * Converted from maven-invoker-plugin tests.
 */
public class BuildTimestampJgitTest extends AbstractTychoIntegrationTest {

	/**
	 * Basic test that builds a feature and bundle with jgit timestamp provider.
	 */
	@Test
	public void testBasic() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/basic.zip");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		File feature = new File(verifier.getBasedir(),
				"feature/target/site/features/buildtimestamp-jgit.feature_1.0.0.201205252029.jar");
		assertTrue("Missing expected feature file: " + feature, feature.canRead());

		File bundle = new File(verifier.getBasedir(),
				"feature/target/site/plugins/buildtimestamp-jgit.bundle_1.0.0.201205252029.jar");
		assertTrue("Missing expected bundle file: " + bundle, bundle.canRead());
	}

	/**
	 * Test with git submodules.
	 */
	@Test
	public void testSubmodules() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/submodules.zip");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		File feature = new File(verifier.getBasedir(),
				"feature/target/site/features/buildtimestamp-jgit.feature_1.0.0.20120701004841.jar");
		assertTrue("Missing expected feature file: " + feature, feature.canRead());

		File bundle = new File(verifier.getBasedir(),
				"feature/target/site/plugins/buildtimestamp-jgit.bundle_1.0.0.20120701004820.jar");
		assertTrue("Missing expected bundle file: " + bundle, bundle.canRead());
	}

	/**
	 * Test that build fails when working tree is dirty and no warning is configured.
	 */
	@Test
	public void testDirtyWorkingTree() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/dirtyworkingtree.zip");
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));
	}

	/**
	 * Test that build fails when submodules are dirty.
	 */
	@Test
	public void testDirtySubmodules() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/dirtySubmodules.zip");
		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")));
	}

	/**
	 * Test with dirty working tree warning only mode.
	 * The qualifier should be newer than the latest commit timestamp.
	 */
	@Test
	public void testDirtyWorkingTreeWarningOnly() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/dirtyWorkingTreeWarningOnly.zip");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		// Find the generated feature jar and extract its qualifier
		File featureDir = new File(verifier.getBasedir(), "feature/target/site/features/");
		Pattern pattern = Pattern.compile("buildtimestamp-jgit\\.feature_1\\.0\\.0\\.(.*)\\.jar");
		File[] files = featureDir.listFiles((dir, name) -> pattern.matcher(name).matches());
		assertTrue("Expected exactly one feature file matching pattern", files != null && files.length == 1);

		Matcher matcher = pattern.matcher(files[0].getName());
		assertTrue("Feature filename should match pattern", matcher.find());
		long qualifier = Long.parseLong(matcher.group(1));
		long latestCommitTimestamp = 201205252029L;
		assertTrue("Qualifier " + qualifier + " must be newer than " + latestCommitTimestamp,
				qualifier > latestCommitTimestamp);

		// The bundle should have the original commit timestamp
		File bundle = new File(verifier.getBasedir(),
				"feature/target/site/plugins/buildtimestamp-jgit.bundle_1.0.0." + latestCommitTimestamp + ".jar");
		assertTrue("Missing expected bundle file: " + bundle, bundle.canRead());
	}

	/**
	 * Test with unrelated dirty submodules (should succeed).
	 */
	@Test
	public void testDirtyUnrelatedSubmodules() throws Exception {
		Verifier verifier = prepareZipTest("buildtimestamp.jgit/dirtyUnrelatedSubmodules.zip");
		verifier.executeGoals(List.of("clean", "verify"));
		verifier.verifyErrorFreeLog();

		File feature = new File(verifier.getBasedir(),
				"feature/target/site/features/buildtimestamp-jgit.unrelateddirtysubmodulefeature_1.0.0.20160314134850.jar");
		assertTrue("Missing expected feature file: " + feature, feature.canRead());

		File bundle = new File(verifier.getBasedir(),
				"feature/target/site/plugins/buildtimestamp-jgit.unrelateddirtysubmodulebundle_1.0.0.20160314131430.jar");
		assertTrue("Missing expected bundle file: " + bundle, bundle.canRead());
	}

	/**
	 * Helper method to prepare a test by extracting a zip file.
	 * 
	 * @param zipPath path relative to tycho-its/projects
	 * @return a Verifier for the extracted test directory
	 */
	private Verifier prepareZipTest(String zipPath) throws Exception {
		File projectsDir = new File(getBasedir(""), "../projects").getCanonicalFile();
		File zipFile = new File(projectsDir, zipPath);
		assertTrue("Zip file not found: " + zipFile, zipFile.exists());

		// Create a unique test directory
		File testDir = new File(getBasedir(""), zipPath.replace(".zip", "")).getCanonicalFile();
		testDir.getParentFile().mkdirs();

		// Extract the zip file
		ZipUnArchiver unzip = new ZipUnArchiver(zipFile);
		unzip.setDestDirectory(testDir);
		unzip.extract();

		return getVerifier(testDir.getAbsolutePath(), false);
	}
}
