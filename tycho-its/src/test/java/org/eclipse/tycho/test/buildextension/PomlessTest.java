package org.eclipse.tycho.test.buildextension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomlessTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBnd() throws Exception {
		Verifier verifier = getVerifier("pomless", false, true);
		verifier.addCliOption("-pl");
		verifier.addCliOption("bnd");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bnd/target/classes/module-info.class");
		assertTrue("module-info.class is not generated!", file.isFile());
	}

	@Test
	public void testPomlessModel() throws Exception {
		// This methods tests:
		// - build.properties (pom.model attributes and properties) are read for:
		// -> Plug-ins, Features, Products(Repos), Targets, Aggregators
		// - tycho.pomless.parent is considered
		// - explicit pom.xml is always preferred

		Verifier verifier = getVerifier("pomless-model", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();

		Map<Path, ModelData> projectData = extractPomModelProperties(Path.of(verifier.getBasedir()));

		assertProjectData("bundles/foo.bar.bundle", projectData, //
				"foo.bar:foo.bar.bundle:1.0.0:eclipse-plugin", "Bundle 1 pomless", "bundle1-pomless");
		assertProjectData("bundles/foo.bar.bundle-2", projectData, //
				"foo.bar:foo.bar.bundle-2:1.0.0:eclipse-plugin", "Bundle 2 from pom.xml", "bundle2-from-pomXML");

		assertProjectData("bundles-2/foo.bar.bundle-5", projectData, //
				"foo.bar:foo.bar.bundle-5:1.0.0:eclipse-plugin", "Bundle 5 pomless", "aggregator2-from-pomXML");

		assertProjectData("bundles-with-enhanced-parents/foo.bar.bundle-3", projectData, //
				"foo.other:foo.bar.bundle-3:1.0.0:eclipse-plugin", "Bundle 3 pomless",
				"alternative-parent-from-pomXML");
		assertProjectData("bundles-with-enhanced-parents/foo.bar.bundle-4", projectData, //
				"bundles-enhanced-pomless:foo.bar.bundle-4:1.0.0:eclipse-plugin", "Bundle 4 pomless",
				"aggregator3-pomless");

		assertProjectData("foo.bar.plugin", projectData, //
				"foo.bar:foo.bar.plugin:1.0.0:eclipse-plugin", "Plugin 1 pomless", "plugin1-pomless");
		assertProjectData("foo.bar.plugin-2", projectData, //
				"foo.bar:foo.bar.plugin-2:1.0.0:eclipse-plugin", "Plugin 2 from pom.xml", "plugin2-from-pomXML");

		assertProjectData("foo.bar.feature", projectData, //
				"foo.bar:foo.bar.feature:1.0.0:eclipse-feature", "Feature 1 pomless", "feature1-pomless");
		assertProjectData("foo.bar.feature-2", projectData, //
				"foo.bar:foo.bar.feature-2:1.0.0:eclipse-feature", "Feature 2 from pom.xml", "feature2-from-pomXML");

		assertProjectData("foo.bar.target", projectData, //
				"foo.bar:foo.bar:1.0.0:eclipse-target-definition", "Target 1 pomless", "target1-pomless");
		assertProjectData("foo.bar.target-2", projectData, //
				"foo.bar:foo.bar.target-2:1.0.0:eclipse-target-definition", "Target 2 from pom.xml",
				"target2-from-pomXML");

		assertProjectData("foo.bar.product", projectData, //
				"foo.bar:foo.bar.product:1.0.0:eclipse-repository", "Product 1 pomless", "product1-pomless");
		assertProjectData("foo.bar.product-2", projectData, //
				"foo.bar:foo.bar.product-2:1.0.0:eclipse-repository", "Product 2 from pom.xml", "product2-from-pomXML");

		assertProjectData(".", projectData, //
				"foo.bar:simple:1.0.0:pom", "simple", "the-default-value");
		assertProjectData("bundles", projectData, //
				"foo.bar:bundles:1.0.0:pom", "[aggregator] bundles", "the-default-value");
		assertProjectData("bundles-2", projectData, //
				"foo.bar:bundles-2:1.1.0:pom", "Aggregator 2 from pomXML", "aggregator2-from-pomXML");
		assertProjectData("bundles-with-enhanced-parents", projectData, //
				"bundles-enhanced-pomless:bundles-with-enhanced-parents:1.2.0:pom", "Aggregator 3 pomless",
				"aggregator3-pomless");

		assertThat(projectData, is(aMapWithSize(0))); // Ensure no more projects are found
	}

	private static Map<Path, ModelData> extractPomModelProperties(Path buildRootDir) throws IOException {
		Map<Path, ModelData> projectData = new HashMap<>();
		try (var paths = Files.walk(buildRootDir).filter(Files::isRegularFile)) {
			var files = paths.filter(p -> "pommodel.data".equals(p.getFileName().toString()));
			for (Path file : (Iterable<Path>) files::iterator) {
				ModelData data = ModelData.extract(file, buildRootDir);
				projectData.put(data.path, data);
			}
		}
		return projectData;
	}

	private static void assertProjectData(String path, Map<Path, ModelData> projectData, String expectedGAV,
			String expectedName, String expectedProperty) {
		ModelData data = projectData.remove(Path.of(path));
		assertEquals(expectedGAV, data.gav);
		assertEquals(expectedName, data.name);
		assertEquals(expectedProperty, data.propertyValue);
	}

	private static final class ModelData {

		static ModelData extract(Path file, Path basedir) throws IOException {
			assertTrue(file.endsWith(Path.of("target", "pommodel.data")));
			Path relativePath = basedir.relativize(file).getParent().getParent();
			Properties properties = new Properties();
			try (var in = Files.newInputStream(file)) {
				properties.load(in);
			}
			assertEquals("Unexpected number of model properties", 3, properties.size());
			String gav = properties.getProperty("GAV");
			String name = properties.getProperty("project.name");
			String propertyValue = properties.getProperty("custom.user.property");
			return new ModelData(relativePath != null ? relativePath : Path.of("."), gav, name, propertyValue);
		}

		final Path path;
		final String gav;
		final String name;
		final String propertyValue;

		public ModelData(Path path, String gav, String name, String propertyValue) {
			this.path = path;
			this.gav = gav;
			this.name = name;
			this.propertyValue = propertyValue;
		}
	}
}
