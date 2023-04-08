package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
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

		Path productsDir = basedir.resolve("products");
		assertTrue(productsDir.toString(), Files.isDirectory(productsDir));
		Path targetDir = productsDir.resolve(productId).resolve("target");
		assertTrue(targetDir.toString(), Files.isDirectory(targetDir));
		Path products2 = targetDir.resolve("products");
		assertTrue(products2.toString(), Files.isDirectory(products2));
		File prodRoot = products2.resolve(productId).toFile();
		assertTrue(prodRoot.toString(), prodRoot.exists());

		if (features.isEmpty()) {
			assertDirectoryDoesNotExist(prodRoot, "*/*/*/features");
		} else {
			for (String featureName : features) {
				// features are unzipped -> directory
				assertDirectoryExists(prodRoot, "*/*/*/features/" + featureName + "_*");
			}
		}
		for (String bundleName : bundles) {
			assertFileExists(prodRoot, "*/*/*/plugins/" + bundleName + "_*.jar");
		}
	}
}
