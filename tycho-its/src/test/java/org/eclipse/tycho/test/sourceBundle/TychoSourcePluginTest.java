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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;

import eu.maveniverse.domtrip.Document;

public class TychoSourcePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBasic() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/basic", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File feature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature_1.0.0.123abc.jar");
		File sourceFeature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature.source_1.0.0.123abc.jar");
		assertTrue(feature.canRead(), "Missing expected file " + feature);
		assertTrue(sourceFeature.canRead(), "Missing expected file " + sourceFeature);

		try (ZipFile featureZip = new ZipFile(feature); ZipFile sourceFeatureZip = new ZipFile(sourceFeature)) {
			assertTrue(findEntry(featureZip, "feature.properties").isPresent(),
					"Missing expected file featrue.properties in " + feature);

			assertTrue(findEntry(sourceFeatureZip, "feature.properties").isPresent(),
					"Content of sourceTemplateFeature not included");

			// Test for bug 552066
			assertTrue(findEntry(featureZip, "license.html").isPresent(), "license.html not found in " + feature);
			assertTrue(findEntry(featureZip, "bin-only.txt").isPresent(), "bin-only.txt not found in " + feature);
			assertTrue(findEntry(featureZip, "src-only.txt").isEmpty(), "src-only.txt found in " + feature);
			assertTrue(findEntry(sourceFeatureZip, "license.html").isPresent(), "license.html not found in " + sourceFeature);
			assertTrue(findEntry(sourceFeatureZip, "bin-only.txt").isEmpty(), "bin-only.txt found in " + sourceFeature);
			assertTrue(findEntry(sourceFeatureZip, "src-only.txt").isPresent(), "src-only.txt not found in " + sourceFeature);
		}
		// Test Bug 374349
		Document sourceFeatureXml = parseFeatureXml(sourceFeature);
		assertEquals("%label", sourceFeatureXml.root().attribute("label"), "Wrong label - bug 374349");
		// Test bug 407706
		assertNull(sourceFeatureXml.root().attributeObject("plugin"));

		File indirectFeature = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/features/sourcefeature.feature.indirect.source_1.0.0.123abc.jar");
		assertTrue(indirectFeature.canRead(), "Missing expected file " + indirectFeature);

		Document indirectFeatureXml = parseFeatureXml(indirectFeature);
//		// Test bug 407706
		assertEquals("sourcefeature.bundle", indirectFeatureXml.root().attribute("plugin"));
		File bundle = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/sourcefeature.bundle.source_1.0.0.123abc.jar");
		assertTrue(bundle.canRead(), "Missing expected file " + bundle);
	}

	private Document parseFeatureXml(File file) throws IOException {
		try (ZipFile indirectFeatureZip = new ZipFile(file)) {
			return Document.of(
					indirectFeatureZip.getInputStream(indirectFeatureZip.getEntry("feature.xml")));
		}
	}

	private static Optional<ZipEntry> findEntry(ZipFile zip, String name) {
		Stream<ZipEntry> stream = StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(zip.entries().asIterator(), Spliterator.ORDERED), false);
		return stream.filter(e -> e.getName().equals(name)).findAny();

	}

	@Test
	public void testExtraSourceBundles() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/extra-source-bundles", true, false);
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/extra.sourcefeature.bundle_1.0.0.123abc.jar");
		assertTrue(file.canRead(), "Missing expected file");
	}

	@Test
	public void testLicenseFeature() throws Exception {
		Verifier verifier = getVerifier("/sourcePlugin/license-feature", false, false);
		verifier.addCliOption("-De342-url=" + ECLIPSE_342.toString());
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File sourceFeature = new File(verifier.getBasedir(), "feature/target/feature-1.0.0-sources-feature.jar");
		assertTrue(sourceFeature.canRead(), "Missing expected file " + sourceFeature);
		ZipFile featureZip = new ZipFile(sourceFeature);
		assertTrue(findEntry(featureZip, "feature.properties").isPresent(),
				"feature.properties not found in " + sourceFeature);
		// test for bug 403950
		assertTrue(findEntry(featureZip, "license.html").isPresent(), "license.html not found in " + sourceFeature);
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
		Verifier verifier = getVerifier("/sourcePlugin/remote-source-bundles", true, false);
		verifier.executeGoals(List.of("clean", "install"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.junit.source_4.13.2.v20240929-1000.jar");
		assertTrue(file.canRead(), "Missing expected file " + file.getName());
		file = new File(verifier.getBasedir(),
				"sourcefeature.repository/target/repository/plugins/org.hamcrest.core.source_2.2.0.v20230809-1000.jar");
		assertTrue(file.canRead(), "Missing expected file " + file.getName());

	}
}
