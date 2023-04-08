/*******************************************************************************
 * Copyright (c) 2022 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.product;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProductTypesTest extends AbstractTychoIntegrationTest {

	private static Verifier testBuildVerifier;

	@BeforeClass
	public static void setupBeforeClass() throws Exception {
		testBuildVerifier = new ProductTypesTest().getVerifier("product.types", false);
		testBuildVerifier.executeGoals(List.of("clean", "verify"));
		testBuildVerifier.verifyErrorFreeLog();
	}

	@Test
	public void testPluginBasedProductsWithUseFeatureAttribute() {
		assertInstalledIUs("bundles1.product", //
				List.of("foo.bar.plugin"), List.of());
	}

	@Test
	public void testPluginBasedProductsWithTypeAttribute() {
		assertInstalledIUs("bundles2.product", //
				List.of("foo.bar.plugin"), List.of());
	}

	@Test
	public void testFeatureBasedProductsWithUseFeatureAttribute() {
		assertInstalledIUs("features1.product", //
				List.of("foo.bar.plugin.in.feature"), List.of("foo.bar.feature"));
	}

	@Test
	public void testFeatureBasedProductsWithTypeAttribute() {
		assertInstalledIUs("features2.product", //
				List.of("foo.bar.plugin.in.feature"), List.of("foo.bar.feature"));
	}

	@Test
	public void testMixedProductsWithTypeAttribute() throws Exception {
		assertInstalledIUs("mixed.product", //
				List.of("foo.bar.plugin", "foo.bar.plugin.in.feature"), List.of("foo.bar.feature"));
	}

	private void assertInstalledIUs(String productId, List<String> bundles, List<String> features) {

		Path basedir = Path.of(testBuildVerifier.getBasedir());

		File prodRoot = basedir.resolve("products").resolve(productId).resolve("target").resolve("products")
				.resolve(productId).toFile();

		if (features.isEmpty()) {
			assertDirectoryDoesNotExist(prodRoot, "**/features");
		} else {
			for (String featureName : features) {
				// features are unzipped -> directory
				assertDirectoryExists(prodRoot, "**/features/" + featureName + "_*");
			}
		}
		for (String bundleName : bundles) {
			assertFileExists(prodRoot, "**/plugins/" + bundleName + "_*.jar");
		}
	}
}
