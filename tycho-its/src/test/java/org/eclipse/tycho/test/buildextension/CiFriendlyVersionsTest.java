/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.test.buildextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.osgi.framework.Constants;

public class CiFriendlyVersionsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dtycho.buildqualifier.format=yyyy");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0." + year + ".jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0." + year);
	}

	@Test
	public void testJgitBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dtycho.buildqualifier.provider=jgit");
		verifier.addCliOption("-Dtycho.buildqualifier.format=yyyyMM");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
		File targetDir = new File(verifier.getBasedir(), "bundle/target");
		String[] jarFiles = targetDir.list((dir, name) -> name.endsWith(".jar"));
		assertEquals(1, jarFiles.length);
		File file = new File(targetDir, jarFiles[0]);
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		String qualifier = jarFiles[0].substring(13, jarFiles[0].lastIndexOf('.'));
		// if formatter fails to parse it will throw exception and thus fail the test
		formatter.parse(qualifier);
		checkManifestVersion(file, "1.0.0." + qualifier);
	}

	@Test
	public void testForcedBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this uses a forced qualifier
		verifier.addCliOption("-DforceContextQualifier=abc");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0.abc.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0.abc");
	}

	@Test
	public void testWithSnapshotBuildQualifier() throws Exception {
		// building with nothing should result in the default -SNAPSHOT build
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0-SNAPSHOT.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
	}

	@Test
	public void testMilestoneBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliOption("-Dqualifier=-M1");
		verifier.addCliOption("-DforceContextQualifier=zzz");
		verifier.addCliOption("-Dtycho.strictVersions=false");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0-M1.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0.zzz");
	}

	@Test
	public void testReleaseBuildWithForcedContextQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this uses force context qualifier set to none
		verifier.addCliOption("-DforceContextQualifier=none");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		checkManifestVersion(file, "1.0.0");
	}

	private static void checkManifestVersion(File file, String expectedVersion) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			assertEquals(expectedVersion, jarFile.getManifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION));
		}
	}

	@Test
	public void testValidateVersionDefaultRevision() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/validateVersion", false);
		Path basedir = Path.of(verifier.getBasedir());

		// Pre-build: verify source MANIFEST.MF and feature.xml have 0.0.6.qualifier
		Path manifest = basedir.resolve("bundles/org.example.bundle/META-INF/MANIFEST.MF");
		assertTrue("MANIFEST.MF should contain 0.0.6.qualifier",
				Files.readString(manifest).contains("Bundle-Version: 0.0.6.qualifier"));
		Path featureXml = basedir.resolve("features/org.example.feature/feature.xml");
		Feature sourceFeature = Feature.read(featureXml.toFile());
		assertEquals("0.0.6.qualifier", sourceFeature.getVersion());

		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		// Post-build: verify built bundle jar has expanded Bundle-Version
		Path bundleJar = basedir.resolve("bundles/org.example.bundle/target/org.example.bundle-0.0.6-SNAPSHOT.jar");
		assertTrue("Bundle jar not found: " + bundleJar, Files.isRegularFile(bundleJar));
		try (JarFile jar = new JarFile(bundleJar.toFile())) {
			String bundleVersion = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION);
			assertTrue("Bundle-Version should start with 0.0.6: " + bundleVersion, bundleVersion.startsWith("0.0.6."));
		}

		// Post-build: verify built feature jar has expanded version
		Path featureJar = basedir.resolve("features/org.example.feature/target/org.example.feature-0.0.6-SNAPSHOT.jar");
		assertTrue("Feature jar not found: " + featureJar, Files.isRegularFile(featureJar));
		Feature builtFeature = Feature.readJar(featureJar.toFile());
		assertTrue("Feature version should start with 0.0.6: " + builtFeature.getVersion(),
				builtFeature.getVersion().startsWith("0.0.6."));
	}

	@Test
	public void testValidateVersionOverriddenRevisionFeature() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/validateVersion", false);
		verifier.addCliOption("-Drevision=0.0.7-SNAPSHOT");
		verifier.addCliOption("-Dtycho.strictVersions=false");
		verifier.addCliOption("-pl features/org.example.feature -am");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		Path basedir = Path.of(verifier.getBasedir());
		Path featureJar = basedir.resolve("features/org.example.feature/target/org.example.feature-0.0.7-SNAPSHOT.jar");
		assertTrue("Feature jar not found: " + featureJar, Files.isRegularFile(featureJar));
		Feature builtFeature = Feature.readJar(featureJar.toFile());
		assertTrue("Feature version should start with 0.0.7: " + builtFeature.getVersion(),
				builtFeature.getVersion().startsWith("0.0.7."));
	}

	@Test
	public void testValidateVersionOverriddenRevisionBundle() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/validateVersion", false);
		verifier.addCliOption("-Drevision=0.0.7-SNAPSHOT");
		verifier.addCliOption("-Dtycho.strictVersions=false");
		verifier.addCliOption("-pl bundles/org.example.bundle -am");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		Path basedir = Path.of(verifier.getBasedir());
		Path bundleJar = basedir.resolve("bundles/org.example.bundle/target/org.example.bundle-0.0.7-SNAPSHOT.jar");
		assertTrue("Bundle jar not found: " + bundleJar, Files.isRegularFile(bundleJar));
		try (JarFile jar = new JarFile(bundleJar.toFile())) {
			String bundleVersion = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION);
			assertTrue("Bundle-Version should start with 0.0.7: " + bundleVersion, bundleVersion.startsWith("0.0.7."));
		}
	}

	@Test
	public void testValidateVersionOverriddenRevisionFullReactor() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/validateVersion", false);
		verifier.addCliOption("-Drevision=0.0.7-SNAPSHOT");
		verifier.addCliOption("-Dtycho.strictVersions=false");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		Path basedir = Path.of(verifier.getBasedir());

		Path bundleJar = basedir.resolve("bundles/org.example.bundle/target/org.example.bundle-0.0.7-SNAPSHOT.jar");
		assertTrue("Bundle jar not found: " + bundleJar, Files.isRegularFile(bundleJar));
		try (JarFile jar = new JarFile(bundleJar.toFile())) {
			String bundleVersion = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_VERSION);
			assertTrue("Bundle-Version should start with 0.0.7: " + bundleVersion, bundleVersion.startsWith("0.0.7."));
		}

		Path featureJar = basedir.resolve("features/org.example.feature/target/org.example.feature-0.0.7-SNAPSHOT.jar");
		assertTrue("Feature jar not found: " + featureJar, Files.isRegularFile(featureJar));
		Feature builtFeature = Feature.readJar(featureJar.toFile());
		assertTrue("Feature version should start with 0.0.7: " + builtFeature.getVersion(),
				builtFeature.getVersion().startsWith("0.0.7."));
	}

}
