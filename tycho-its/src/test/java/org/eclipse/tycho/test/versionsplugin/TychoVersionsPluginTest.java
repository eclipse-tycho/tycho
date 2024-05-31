/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.versionsplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.it.Verifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.version.TychoVersion;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class TychoVersionsPluginTest extends AbstractTychoIntegrationTest {

	private static final String VERSION = TychoVersion.getTychoVersion();

	private static final Pattern VERSION_PATTERN = Pattern.compile("version=\"(.*)\"");

	/**
	 * <p>
	 * This test verifies that current and future versions of the
	 * tycho-versions-plugin can be executed on a project that is built with Tycho
	 * 0.12.0. With this assertion it's possible to call the plugin without version
	 * on the commandline:
	 * </p>
	 * <p>
	 * <code>mvn org.eclipse.tycho:tycho-versions-plugin:set-version</code>
	 * </p>
	 * <p>
	 * Background: The tycho-versions-plugin 0.12.0 can't handle projects that are
	 * built with Tycho 0.11.0 or older, see
	 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=363791">Bug
	 * 363791</a>.
	 * </p>
	 */
	@Test
	public void invokeVersionsPluginOnTycho0120Project() throws Exception {
		String expectedNewVersion = "1.2.3";

		Verifier verifier = getVerifier("tycho-version-plugin/set-version/compat", true);

		verifier.addCliOption("-DnewVersion=" + expectedNewVersion);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();

		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "pom.xml")));
		assertEquals("<version> in pom.xml has not been changed!", expectedNewVersion, pomModel.getVersion());
	}

	@Test
	public void updateTargetVersionTest() throws Exception {
		String expectedNewVersion = "1.2.3";

		Verifier verifier = getVerifier("tycho-version-plugin/set-version/update-target", true);

		verifier.addCliOption("-DnewVersion=" + expectedNewVersion);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();
		String targetContent = Files.readString(new File(verifier.getBasedir(), "including/including.target").toPath());
		assertTrue("Actual Target content = " + targetContent,
				targetContent.contains("mvn:org.tycho.its:other:" + expectedNewVersion + ":target")
						&& targetContent.contains("sequenceNumber=\"12\""));
	}

	@Test
	public void updateProjectVersionBndTest() throws Exception {
		String expectedNewVersion = "1.2.3";

		Verifier verifier = getVerifier("tycho-version-plugin/set-version/pde-bnd", true);

		verifier.addCliOption("-DnewVersion=" + expectedNewVersion);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();
		Properties properties = new Properties();
		properties.load(Files.newInputStream(new File(verifier.getBasedir(), "bundle/pde.bnd").toPath()));
		String versionProperty = properties.getProperty("Bundle-Version");
		assertNotNull("Bundle-Version is null", versionProperty);
		assertEquals("Bundle-Version is not as expected!", expectedNewVersion, versionProperty);
	}

	@Test
	public void updateProjectVersionWithNestedPom() throws Exception {
		String expectedNewVersion = "1.1.0";

		Verifier verifier = getVerifier("tycho-version-plugin/set-version/nested_modules", true);

		verifier.addCliOption("-DnewVersion=" + expectedNewVersion);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();
		String bundlePom = "bundle/pom.xml";
		List<String> poms = List.of("pom.xml", "parent/pom.xml", bundlePom);
		for (String pom : poms) {
			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), pom)));
			if (bundlePom.equals(pom)) {
				assertNull("child should inherit version from parent", pomModel.getVersion());
				Parent parent = pomModel.getParent();
				assertNotNull("project > parent is null", parent);
				assertEquals("project > parent > version in " + pom + " has not been changed!", expectedNewVersion,
						parent.getVersion());
			} else {
				assertEquals("project > version in " + pom + " has not been changed!", expectedNewVersion,
						pomModel.getVersion());
			}
		}
		Manifest manifest = getManifest(verifier, "bundle");
		assertEquals("version in manifest was not updated!", expectedNewVersion,
				manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
	}

	@Test
	public void updateProjectVersionOnlyChangesVersionOfNestedProjectsIfSameVersionAsRoot() throws Exception {
		Verifier verifier = getVerifier("tycho-version-plugin/set-version/only_same_version", false);

		verifier.addCliOption("-DnewVersion=1.0.1");
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();

		record Expectation(String pom, String expectedVersion, String expectedParentVersion) {
		}
		List<Expectation> expectations = List.of( //
				new Expectation("pom.xml", "1.0.1", null), //
				new Expectation("p/pom.xml", "1.0.1", "1.0.1"), //
				new Expectation("p/m1/pom.xml", "1.0.1", "1.0.1"), //
				new Expectation("q/pom.xml", "2.0.0", "1.0.1"), // only parent shall be changed
				new Expectation("q/m2/pom.xml", "2.0.0", "2.0.0") // nothing shall be changed
		);
		for (Expectation expectation : expectations) {
			MavenXpp3Reader pomReader = new MavenXpp3Reader();
			String pom = expectation.pom();
			Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), pom)));
			Parent parent = pomModel.getParent();

			assertEquals("project > version in " + pom + " is not as expected!", expectation.expectedVersion(),
					pomModel.getVersion());
			if (expectation.expectedParentVersion() == null) {
				assertNull("project > parent in " + pom + " should be null", parent);
			} else {
				assertEquals("project > parent > version in " + pom + " is not as expected!",
						expectation.expectedParentVersion(), parent.getVersion());
			}
		}
		assertEquals("version in manifest p/m1 is not as expected!", "1.0.1",
				getManifest(verifier, "p/m1").getMainAttributes().getValue(Constants.BUNDLE_VERSION));
		assertEquals("version in manifest q/m2 is not as expected!", "2.0.0",
				getManifest(verifier, "q/m2").getMainAttributes().getValue(Constants.BUNDLE_VERSION));
	}

	@Test
	public void updateVersionRanges() throws Exception {
		String expectedNewMavenVersion = "1.1.0-SNAPSHOT";
		String expectedNewOSGiVersion = "1.1.0.qualifier";
		String expectedPackageVersion = "1.1.0";
		String expectedMicroVersionRange = "[" + expectedPackageVersion + ",1.1.1)";
		String expectedNarrowVersionRange = "[" + expectedPackageVersion + ",1.2.0)";
		String expectedWideVersionRange = "[" + expectedPackageVersion + ",2)";
		// example call:
		// mvn org.eclipse.tycho:tycho-versions-plugin:5.0.0-SNAPSHOT:set-version
		// -DnewVersion=1.1.0-SNAPSHOT
		// -DupdateVersionRangeMatchingBounds
		Verifier verifier = getVerifier("tycho-version-plugin/set-version/version_ranges", true);
		verifier.addCliOption("-DnewVersion=" + expectedNewMavenVersion);
		verifier.addCliOption("-DupdateVersionRangeMatchingBounds");
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");
		{// check the package itself is updated
			Manifest provider = getManifest(verifier, "provider.bundle");
			assertEquals("version in manifest was not updated for provider bundle!", expectedNewOSGiVersion,
					provider.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
			assertVersion(provider, expectedPackageVersion, Constants.EXPORT_PACKAGE);
		}
		{// check open range is updated
			Manifest consumerOpen = getManifest(verifier, "consumer.open");
			assertEquals("version in manifest was not updated for open consumer bundle!", expectedNewOSGiVersion,
					consumerOpen.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
			assertVersion(consumerOpen, expectedPackageVersion, Constants.IMPORT_PACKAGE);
			assertVersion(consumerOpen, expectedPackageVersion, Constants.REQUIRE_BUNDLE);
		}
		{// check wide version range is updated
			Manifest consumerWide = getManifest(verifier, "consumer.wide");
			assertEquals("version in manifest was not updated for wide consumer bundle!", expectedNewOSGiVersion,
					consumerWide.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
			assertVersionRange(consumerWide, expectedWideVersionRange, Constants.IMPORT_PACKAGE);
			assertVersionRange(consumerWide, expectedWideVersionRange, Constants.REQUIRE_BUNDLE);
		}
		{// check narrow version range is updated
			Manifest consumerNarrow = getManifest(verifier, "consumer.narrow");
			assertEquals("version in manifest was not updated for narrow consumer bundle!", expectedNewOSGiVersion,
					consumerNarrow.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
			assertVersionRange(consumerNarrow, expectedNarrowVersionRange, Constants.IMPORT_PACKAGE);
			assertVersionRange(consumerNarrow, expectedNarrowVersionRange, Constants.REQUIRE_BUNDLE);
		}
		{// check micro version range is updated
			Manifest consumerNarrow = getManifest(verifier, "consumer.micro");
			assertEquals("version in manifest was not updated for micro consumer bundle!", expectedNewOSGiVersion,
					consumerNarrow.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
			assertVersionRange(consumerNarrow, expectedMicroVersionRange, Constants.IMPORT_PACKAGE);
			assertVersionRange(consumerNarrow, expectedMicroVersionRange, Constants.REQUIRE_BUNDLE);
		}
	}

	private Manifest getManifest(Verifier verifier, String bundle) throws IOException, FileNotFoundException {
		try (FileInputStream stream = new FileInputStream(
				new File(verifier.getBasedir(), bundle + "/" + JarFile.MANIFEST_NAME))) {
			return new Manifest(stream);
		}
	}

	@Test
	public void updateProjectMetadataVersionBndTest() throws Exception {
		String expectedNewVersion = "2.0.0.qualifier";

		Verifier verifier = getVerifier("tycho-version-plugin/update-eclipse-metadata/pde-bnd", false, false);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":update-eclipse-metadata");
		verifier.verifyErrorFreeLog();
		Properties properties = new Properties();
		properties.load(Files.newInputStream(new File(verifier.getBasedir(), "pde.bnd").toPath()));
		String versionProperty = properties.getProperty("Bundle-Version");
		assertNotNull("Bundle-Version is null", versionProperty);
		assertEquals("Bundle-Version is not as expected!", expectedNewVersion, versionProperty);
	}

	/**
	 * Verifies that the update-pom goal of the tycho-version plug-in updates the
	 * version of a pom when the pom file is implicit. The command line for this
	 * would be
	 * <p>
	 * <code>mvn org.eclipse.tycho:tycho-versions-plugin:update-pom</code>
	 * </p>
	 * This was created in response to
	 * <a href="https://github.com/eclipse-tycho/tycho/issues/309">issue 309</a>.
	 */
	@Test
	public void testUpdatePomWithImplicitPomName() throws Exception {
		String POM_NAME = "pom.xml";
		String MANIFEST_VERSION = "2.0.0";

		Verifier verifier = getVerifier("tycho-version-plugin/update-pom/pomNamedPomXml", false);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:%s:update-pom".formatted(VERSION));
		verifier.verifyErrorFreeLog();
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), POM_NAME)));
		assertEquals("<version> in pom.xml has not been changed!", MANIFEST_VERSION, pomModel.getVersion());
	}

	/**
	 * Verifies that the update-pom goal of the tycho-version plug-in updates the
	 * version of a pom that is named 'pom.xml'. The command line for this would be
	 * <p>
	 * <code>mvn -f pom.xml org.eclipse.tycho:tycho-versions-plugin:update-pom</code>
	 * </p>
	 * This was created in response to
	 * <a href="https://github.com/eclipse-tycho/tycho/issues/309">issue 309</a>.
	 */
	@Test
	public void testUpdatePomOfPomThatIsNamedPomXml() throws Exception {
		String POM_NAME = "pom.xml";
		String MANIFEST_VERSION = "2.0.0";

		Verifier verifier = getVerifier("tycho-version-plugin/update-pom/pomNamedPomXml", false);
		verifier.addCliOption("--file " + POM_NAME);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:%s:update-pom".formatted(VERSION));
		verifier.verifyErrorFreeLog();
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), POM_NAME)));
		assertEquals("<version> in pom.xml has not been changed!", MANIFEST_VERSION, pomModel.getVersion());
	}

	/**
	 * Verifies that the update-pom goal of the tycho-version plug-in updates the
	 * version of a pom that is NOT named 'pom.xml'. The command line for this would
	 * be
	 * <p>
	 * <code>mvn -f foo.bar.pom.xml org.eclipse.tycho:tycho-versions-plugin:update-pom</code>
	 * </p>
	 * This was created in response to
	 * <a href="https://github.com/eclipse-tycho/tycho/issues/309">issue 309</a>.
	 */
	@Test
	public void testUpdatePomOfPomThatIsNotNamedPomXml() throws Exception {
		String POM_NAME = "foo.bar.pom.xml";
		String MANIFEST_VERSION = "2.0.0";

		Verifier verifier = getVerifier("tycho-version-plugin/update-pom/pomNotNamedPomXml", false);
		verifier.addCliOption("--file " + POM_NAME);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:%s:update-pom".formatted(VERSION));
		verifier.verifyErrorFreeLog();
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), POM_NAME)));
		assertEquals("<version> in foo.bar.pom.xml has not been changed!", MANIFEST_VERSION, pomModel.getVersion());
	}

	/**
	 * Verifies that the update-pom goal of the tycho-version plug-in updates the
	 * version of poms references in a modular pom. It tests modules with the
	 * implicit name, an explicit default name and a custom name. The command line
	 * for this would be
	 * <p>
	 * <code>mvn -f aggregate org.eclipse.tycho:tycho-versions-plugin:update-pom</code>
	 * </p>
	 * This was created in response to
	 * <a href="https://github.com/eclipse-tycho/tycho/issues/309">issue 309</a>.
	 */
	@Test
	public void testUpdatePomsOfModularPom() throws Exception {
		String POM_NAME = "aggregate";
		String MANIFEST_VERSION = "2.0.0";

		Verifier verifier = getVerifier("tycho-version-plugin/update-pom/modularPom", false);
		verifier.addCliOption("--file " + POM_NAME);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:%s:update-pom".formatted(VERSION));
		verifier.verifyErrorFreeLog();
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomImplicit = pomReader.read(new FileReader(file(verifier, "defaultPomNameA", "pom.xml")));
		Model pomDefault = pomReader.read(new FileReader(file(verifier, "defaultPomNameB", "pom.xml")));
		Model pomCustom = pomReader.read(new FileReader(file(verifier, "customPomName", "customPomName.xml")));
		Model pomDeepNest = pomReader.read(new FileReader(file(verifier, "deepNest", "a", "b", "deepNest.xml")));

		assertEquals("<version> in defaultPomNameA/pom.xml has not been changed!", MANIFEST_VERSION,
				pomImplicit.getVersion());
		assertEquals("<version> in defaultPomNameB/pom.xml has not been changed!", MANIFEST_VERSION,
				pomDefault.getVersion());
		assertEquals("<version> in customPomName/customPomName.xml has not been changed!", MANIFEST_VERSION,
				pomCustom.getVersion());
		assertEquals("<version> in deepNest/a/b/deepNest.xml has not been changed!", MANIFEST_VERSION,
				pomDeepNest.getVersion());

	}

	@Test
	public void testCiFriendlyVersion() throws Exception {
		String expectedNewVersion = "2.0.0-SNAPSHOT";
		String expectedNewOSGiVersion = "2.0.0.qualifier";

		Verifier verifier = getVerifier("tycho-version-plugin/set-version/ci_friendly", false);

		verifier.addCliOption("-DnewVersion=" + expectedNewVersion);
		verifier.executeGoal("org.eclipse.tycho:tycho-versions-plugin:" + VERSION + ":set-version");

		verifier.verifyErrorFreeLog();

		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model pomModel = pomReader.read(new FileReader(new File(verifier.getBasedir(), "pom.xml")));
		assertEquals("${revision}", pomModel.getVersion());
		assertEquals(expectedNewVersion, pomModel.getProperties().getProperty("revision"));
		Manifest manifest = getManifest(verifier, ".");
		assertEquals(expectedNewOSGiVersion, manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
	}

	public static File file(Verifier verifier, String... path) {
		return Path.of(verifier.getBasedir(), path).toFile();
	}

	private static void assertVersionRange(Manifest manifest, String versionRange, String header) {
		String value = manifest.getMainAttributes().getValue(header);
		assertNotNull("Header " + header + " not found", value);
		Matcher matcher = VERSION_PATTERN.matcher(value);
		assertTrue("no version found on " + value, matcher.find());
		VersionRange expected = VersionRange.valueOf(versionRange);
		VersionRange actual = VersionRange.valueOf(matcher.group(1));
		assertTrue(header + " " + value + ": expected version range = " + expected + " but actual version range = "
				+ actual, expected.equals(actual));
	}

	private static void assertVersion(Manifest manifest, String version, String header) {
		String value = manifest.getMainAttributes().getValue(header);
		assertNotNull("Header " + header + " not found", value);
		Matcher matcher = VERSION_PATTERN.matcher(value);
		assertTrue("no version found on " + value, matcher.find());
		Version expected = Version.valueOf(version);
		Version actual = Version.valueOf(matcher.group(1));
		assertTrue(header + " " + value + ": expected version = " + expected + " but actual version = " + actual,
				expected.equals(actual));
	}

}
