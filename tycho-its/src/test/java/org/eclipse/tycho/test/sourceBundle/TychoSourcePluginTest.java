/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.sourceBundle;

import static org.eclipse.tycho.test.util.ResourceUtil.P2Repositories.ECLIPSE_342;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class TychoSourcePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBasic() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/basic", false, false);
		verifier.addCliArgument("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File feature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature_1.0.0.123abc.jar");
		File sourceFeature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature.source_1.0.0.123abc.jar");
		assertTrue("Missing expected file " + feature, feature.canRead());
		assertTrue("Missing expected file " + sourceFeature, sourceFeature.canRead());

		try (ZipFile featureZip = new ZipFile(feature); ZipFile sourceFeatureZip = new ZipFile(sourceFeature)) {
			assertTrue("Missing expected file featrue.properties in " + feature,
					findEntry(featureZip, "feature.properties").isPresent());

			assertTrue("Content of sourceTemplateFeature not included",
					findEntry(sourceFeatureZip, "feature.properties").isPresent());

			// Test for bug 552066
			assertTrue("license.html not found in " + feature, findEntry(featureZip, "license.html").isPresent());
			assertTrue("bin-only.txt not found in " + feature, findEntry(featureZip, "bin-only.txt").isPresent());
			assertTrue("src-only.txt found in " + feature, findEntry(featureZip, "src-only.txt").isEmpty());
			assertTrue("license.html not found in " + sourceFeature,
					findEntry(sourceFeatureZip, "license.html").isPresent());
			assertTrue("bin-only.txt found in " + sourceFeature, findEntry(sourceFeatureZip, "bin-only.txt").isEmpty());
			assertTrue("src-only.txt not found in " + sourceFeature,
					findEntry(sourceFeatureZip, "src-only.txt").isPresent());
		}
		// Test Bug 374349
		Document sourceFeatureXml = parseFeatureXml(sourceFeature);
		assertEquals("Wrong label - bug 374349", "%label",
				sourceFeatureXml.getChild("feature").getAttributeValue("label"));
		// Test bug 407706
		assertNull(sourceFeatureXml.getChild("feature").getAttribute("plugin"));

		File indirectFeature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature.indirect.source_1.0.0.123abc.jar");
		assertTrue("Missing expected file " + indirectFeature, indirectFeature.canRead());

		Document indirectFeatureXml = parseFeatureXml(indirectFeature);
//		// Test bug 407706
		assertEquals("sourcefeature.bundle", indirectFeatureXml.getChild("feature").getAttributeValue("plugin"));
		File bundle = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/sourcefeature.bundle.source_1.0.0.123abc.jar");
		assertTrue("Missing expected file " + bundle, bundle.canRead());
	}

	private Document parseFeatureXml(File file) throws IOException {
		try (ZipFile indirectFeatureZip = new ZipFile(file)) {
			return new XMLParser().parse(
					new XMLIOSource(indirectFeatureZip.getInputStream(indirectFeatureZip.getEntry("feature.xml"))));
		}
	}

	private static Optional<ZipEntry> findEntry(ZipFile zip, String name) {
		Stream<ZipEntry> stream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(zip.entries().asIterator(), Spliterator.ORDERED), false);
		return stream.filter(e -> e.getName().equals(name)).findAny();

	}

	@Test
	public void testExtraSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/extra-source-bundles", false, false);
		verifier.addCliArgument("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/extra.sourcefeature.bundle_1.0.0.123abc.jar");
		assertTrue("Missing expected file", file.canRead());
	}

	@Test
	public void testLicenseFeature() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/license-feature", false, false);
		verifier.addCliArgument("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File sourceFeature = new File(verifier.getBasedir(), "feature/target/feature-1.0.0-sources-feature.jar");
		assertTrue("Missing expected file " + sourceFeature, sourceFeature.canRead());
		ZipFile featureZip = new ZipFile(sourceFeature);
		assertTrue("feature.properties not found in " + sourceFeature,
				findEntry(featureZip, "feature.properties").isPresent());
		// test for bug 403950
		assertTrue("license.html not found in " + sourceFeature, findEntry(featureZip, "license.html").isPresent());
		// test bug 395773
		Properties actual = new Properties();
		actual.load(featureZip.getInputStream(featureZip.getEntry("feature.properties")));

		// content must be merged from 1. license feature, 2. feature, 3. sourceTemplate
		assertEquals("feature label Developer Resources", actual.getProperty("label"));
		assertEquals("source feature description", actual.getProperty("description"));
		assertEquals("license feature copyright", actual.getProperty("copyright"));
		assertEquals("license.html", actual.getProperty("licenseURL"));
		assertEquals("license feature license", actual.getProperty("license"));
	}

	@Test
	public void testRemoteSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/remote-source-bundles", false, false);
		verifier.addCliArgument("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_3.8.2.v3_8_2_v20100427-1100.jar");
		assertTrue("Missing expected file " + file.getName(), file.canRead());
		file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_4.8.1.v4_8_1_v20100427-1100.jar");
		assertTrue("Missing expected file " + file.getName(), file.canRead());

	}
}
