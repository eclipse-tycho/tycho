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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.version.TychoVersion;
import org.junit.Test;

public class TychoVersionsPluginTest extends AbstractTychoIntegrationTest {

	private static final String VERSION = TychoVersion.getTychoVersion();

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

	public static File file(Verifier verifier, String... path) {
		return Path.of(verifier.getBasedir(), path).toFile();
	}

}
