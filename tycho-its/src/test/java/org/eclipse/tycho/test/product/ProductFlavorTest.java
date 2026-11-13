/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProductFlavorTest extends AbstractTychoIntegrationTest {

	private static Verifier verifier;

	@BeforeClass
	public static void buildProduct() throws Exception {
		verifier = new ProductFlavorTest().getVerifier("eclipserun.flavor", false);

		verifier.executeGoals(Arrays.asList("clean", "install"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void testProductStart6() throws Exception {
		runTest("start6", "6", "true");
	}

	@Test
	public void testProductStart4() throws Exception {
		runTest("start4", "4", "true");
	}

	@Test
	public void testProductNoConfig() throws Exception {
		runTest("noconfig", "4", "false");
	}

	private void runTest(String name, String startLevel, String autoStart) throws Exception {
		Path repoBaseDir = Path.of(verifier.getBasedir());
		Path repoPath = repoBaseDir.resolve("repository");

		Verifier testVerifier = getVerifier("eclipserun.flavor/eclipserun", false);
		testVerifier.setEnvironmentVariable("PRODUCT_FLAVOR", name);
		testVerifier.setEnvironmentVariable("PRODUCT_DESTINATION", name);
		testVerifier.setEnvironmentVariable("PRODUCT_INSTALLIU", name + ".product.id");
		testVerifier.setEnvironmentVariable("PRODUCT_REPOSITORY", repoPath.toUri().toString());
		testVerifier.executeGoal("package");
		testVerifier.verifyErrorFreeLog();

		Path baseDir = Path.of(testVerifier.getBasedir());
		File configFile = baseDir.resolve(name).resolve("configuration/org.eclipse.equinox.simpleconfigurator")
				.toFile();

		File[] bundleInfoFiles = assertFileExists(configFile, "bundles.info");
		for (String bundleInfo : Files.readAllLines(bundleInfoFiles[0].toPath(), StandardCharsets.UTF_8)) {
			String[] parts = bundleInfo.split(",");
			if (parts.length == 5 && "flavor.example.bundle".equals(parts[0])) {
				assertEquals("Start level of example.bundle in product '" + name + "' does not match", startLevel,
						parts[3]);
				assertEquals("Autostart of example.bundle in product '" + name + "' does not match", autoStart,
						parts[4]);
				return;
			}
		}
		fail("'example.bundle' was not found in product '" + name + "' in simpleconfigurator bundles.info");
	}
}
