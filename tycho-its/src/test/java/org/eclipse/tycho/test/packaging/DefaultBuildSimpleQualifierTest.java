/*******************************************************************************
 * Copyright (c) 2016, 2021 Bachmann electronic GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.packaging;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test uses a project that contains 3 plugins (2 eclipse-plugins and one
 * maven plugin). The maven plugin contains a text file where the
 * maven.build.buildtimestamp is written to. It compares the content of that
 * file with the qualifier of the Bundle-Version in the eclipse plugins manifest
 * files.
 *
 * The test is ensuring qualifier to be presented
 */
public class DefaultBuildSimpleQualifierTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaulBuildTimestampIsTheMavenBuildTimestamp() throws Exception {
		Verifier verifier = getVerifier("/packaging.buildsimplequalifier", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
		File baseDir = new File(verifier.getBasedir());

		String plugin1Manifest = Files.readString(Paths.get(baseDir.getAbsolutePath(), "plugin01/target/MANIFEST.MF"));
		String plugin2Manifest = Files.readString(Paths.get(baseDir.getAbsolutePath(), "plugin02/target/MANIFEST.MF"));
		String expectedBundle1Version = "Bundle-Version: 1.0.0.1";
		String expectedBundle2Version = "Bundle-Version: 1.0.1.1";
		Assert.assertTrue(
				"Expected Bundle-Version in MANIFEST: '" + expectedBundle1Version + "'\nbut was\n" + plugin1Manifest,
				plugin1Manifest.contains(expectedBundle1Version));

		Assert.assertTrue(
				"Expected Bundle-Version in MANIFEST: '" + expectedBundle2Version + "'\nbut was\n" + plugin2Manifest,
				plugin2Manifest.contains(expectedBundle2Version));
	}

}
