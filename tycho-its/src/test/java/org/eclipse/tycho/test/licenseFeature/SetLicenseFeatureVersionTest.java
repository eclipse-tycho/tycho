/*******************************************************************************
 * Copyright (c) 2014, 2018 Darmstadt University of Technology.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.licenseFeature;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// tests that license feature references are updated by the versions-plugin (bug 424945)
// TODO make this a unit test
public class SetLicenseFeatureVersionTest extends AbstractTychoIntegrationTest {

	private static final String NEW_MAVEN_VERSION = "1.0.1-SNAPSHOT";
	private static final String NEW_OSGI_VERSION = "1.0.1.qualifier";

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/licenseFeature.setVersion", false);

		verifier.addCliArgument("-DnewVersion=" + NEW_MAVEN_VERSION);
		verifier.executeGoal(
				"org.eclipse.tycho:tycho-versions-plugin:" + TychoVersion.getTychoVersion() + ":set-version");

		verifier.verifyErrorFreeLog();

		File licenseFeatureDir = new File(verifier.getBasedir(), "license.feature");
		Feature licenseFeature = Feature.read(new File(licenseFeatureDir, "feature.xml"));
		assertEquals(NEW_OSGI_VERSION, licenseFeature.getVersion());

		File otherFeatureDir = new File(verifier.getBasedir(), "other.feature");
		Feature otherFeature = Feature.read(new File(otherFeatureDir, "feature.xml"));
		assertEquals(NEW_OSGI_VERSION, otherFeature.getLicenseFeatureVersion());
	}
}
