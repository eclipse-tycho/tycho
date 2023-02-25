package org.eclipse.tycho.test.buildextension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PomlessTest extends AbstractTychoIntegrationTest {

	@Test
	public void testBnd() throws Exception {
		Verifier verifier = getVerifier("pomless", false, true);
		verifier.addCliArgument("-pl");
		verifier.addCliArgument("bnd");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bnd/target/classes/module-info.class");
		assertTrue("module-info.class is not generated!", file.isFile());
	}

	@Test
	public void testPomlessTestPluginDetection() throws Exception {

		Verifier verifier = getVerifier("pomless-tests", false, true);
		verifier.executeGoals(List.of("clean", "test"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("");

		Map<Path, ModelData> projectData = extractPomModelProperties(verifier);

		assertPackagingTypeData("tests/foo.checks", projectData, //
				"foo.checks", "eclipse-plugin");
		assertPackagingTypeData("tests/foo.not.tests", projectData, //
				"foo.not.tests", "eclipse-plugin");
		assertPackagingTypeData("tests/foo.test", projectData, //
				"foo.test", "eclipse-test-plugin");
		assertPackagingTypeData("tests/foo.tests", projectData, //
				"foo.tests", "eclipse-test-plugin");
		assertPackagingTypeData("tests/foo.tests.topic", projectData, //
				"foo.tests.topic", "eclipse-test-plugin");

		assertPackagingTypeData(".", projectData, //
				"simple", "pom");
		assertPackagingTypeData("tests", projectData, //
				"tests", "pom");

		assertThat(projectData, is(aMapWithSize(0))); // Ensure no more projects are found
	}

	private static void assertPackagingTypeData(String path, Map<Path, ModelData> projectData, String artifactId,
			String packaingType) {
		String expectedGAV = "foo.bar:" + artifactId + ":1.0.0:" + packaingType;
		ModelData data = projectData.remove(Path.of(path));
		assertEquals(1, data.properties.size(), "Unexpected number of model properties");
		assertEquals(expectedGAV, data.properties.get("GAV"));
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

		Map<Path, ModelData> projectData = extractPomModelProperties(verifier);

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

	private static void assertProjectData(String path, Map<Path, ModelData> projectData, String expectedGAV,
			String expectedName, String expectedProperty) {
		ModelData data = projectData.remove(Path.of(path));
		assertEquals(3, data.properties.size(), "Unexpected number of model properties");
		assertEquals(expectedGAV, data.properties.get("GAV"));
		assertEquals(expectedName, data.properties.get("project.name"));
		assertEquals(expectedProperty, data.properties.get("custom.user.property"));
	}

	private Map<Path, ModelData> extractPomModelProperties(Verifier verifier) throws IOException {
		Path buildRootDir = Path.of(verifier.getBasedir());
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

	private static final class ModelData {

		static ModelData extract(Path file, Path basedir) throws IOException {
			assertTrue(file.endsWith(Path.of("target", "pommodel.data")));
			Path relativePath = basedir.relativize(file).getParent().getParent();
			Properties properties = new Properties();
			try (var in = Files.newInputStream(file)) {
				properties.load(in);
			}
			return new ModelData(relativePath != null ? relativePath : Path.of("."), properties);
		}

		final Path path;
		final Map<String, String> properties;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ModelData(Path path, Properties properties) {
			this.path = path;
			this.properties = (Map) properties;
		}
	}
}
