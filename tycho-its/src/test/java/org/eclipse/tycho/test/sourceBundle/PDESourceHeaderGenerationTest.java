package org.eclipse.tycho.test.sourceBundle;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Constants;

class PDESourceHeaderGenerationTest extends AbstractTychoIntegrationTest {

	@Test
	void testSourceHeaderGeneration() throws Exception {
		Verifier verifier = getVerifier("mixed.reactor", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		Path basedir = Path.of(verifier.getBasedir());
		Path sourcesJar = basedir
				.resolve(Path.of("felix.bundle", "target", "felix.bundle-1.0.0-SNAPSHOT-sources-pde.jar"));

		assertTrue(Files.isRegularFile(sourcesJar));
		try (JarFile jar = new JarFile(sourcesJar.toFile())) {
			Attributes mainAttributes = jar.getManifest().getMainAttributes();
			assertEquals("org.eclipse.tycho.it.felix.bundle;version=\"1.0.0.SNAPSHOT\";roots:=\".\"",
					mainAttributes.getValue("Eclipse-SourceBundle"));
			assertEquals("%bundleName", mainAttributes.getValue(Constants.BUNDLE_NAME));
			assertEquals("%bundleVendor", mainAttributes.getValue(Constants.BUNDLE_VENDOR));
			assertEquals("OSGI-INF/l10n/bundle-src", mainAttributes.getValue(Constants.BUNDLE_LOCALIZATION));

			Properties props = new Properties();
			try (var localProps = jar.getInputStream(jar.getEntry("OSGI-INF/l10n/bundle-src.properties"))) {
				props.load(localProps);
			}
			assertEquals("The Felix Bundle Source", props.getProperty("bundleName"));
			assertEquals("The Bundle House", props.getProperty("bundleVendor"));
		}

	}

}
