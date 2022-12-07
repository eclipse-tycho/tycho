/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.TYCHO0453expandReleaseVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExpandReleaseVersionTest extends AbstractTychoIntegrationTest {
	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("/TYCHO0453expandReleaseVersion", false);
		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		File featureXml = new File(verifier.getBasedir(), "feature/target/feature.xml");
		Feature feature = Feature.read(featureXml);
		assertEquals("1.0.0.1234567890-bundle", feature.getPlugins().get(0).getVersion());

		File contentXml = new File(verifier.getBasedir(), "site/target/targetPlatformRepository/content.xml");
		String contentXmlString = Files.readString(contentXml.toPath());
		assertTrue(contentXmlString.contains("unit id='feature.feature.jar' version='1.0.0.1234567890-feature'"));

	}

}
