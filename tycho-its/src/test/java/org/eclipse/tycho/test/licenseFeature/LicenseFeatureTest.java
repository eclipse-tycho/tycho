/*******************************************************************************
 * Copyright (c) 2012, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - Bug #519941 Copy the shared license info to target feature.xml
 *******************************************************************************/
package org.eclipse.tycho.test.licenseFeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

// tests the license feature support (bug 368985)
public class LicenseFeatureTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/licenseFeature", true);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();

		assertFeatureJar(
				new File(verifier.getBasedir(), "repository/target/repository/features/feature_1.2.3.123abc.jar"));
		assertFeatureJar(new File(verifier.getBasedir(),
				"repository/target/repository/features/feature.conflicting-dependencies_1.2.3.123abc.jar"));
		assertFeatureJar(new File(verifier.getBasedir(),
				"repository/target/repository/features/feature.without-properties_1.2.3.123abc.jar"));
	}

	protected void assertFeatureJar(File feature) throws ZipException, IOException {
		assertTrue(feature.canRead());

		try (ZipFile zip = new ZipFile(feature)) {

			Assert.assertNotNull(zip.getEntry("file1.txt"));
			Assert.assertNotNull(zip.getEntry("file2.txt"));

			Properties p = new Properties();

			try (InputStream is = zip.getInputStream(zip.getEntry("feature.properties"))) {
				p.load(is);
			}

			Feature featureXML = Feature.readJar(feature);

			// make sure that the properties file contains the keys
			assertEquals("file1.txt", p.getProperty("licenseURL"));
			assertEquals("License - The More The Merrier.", p.getProperty("license"));

			// make sure that the feature.xml references the keys from the properties file
			assertEquals("%licenseURL", featureXML.getLicenseURL());
			assertEquals("%license", featureXML.getLicense().trim());
		}
	}

}
