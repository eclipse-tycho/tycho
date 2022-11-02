package org.eclipse.tycho.test.packaging;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ManifestAndFeatureTest extends AbstractTychoIntegrationTest {
    @Test
    public void testManifestHasBeenFilterer() throws Exception {
        final Verifier verifier = getVerifier("/packaging.manifestAndFeature", false);
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        final Path targetManifestPath = Path.of(verifier.getBasedir(), "target/MANIFEST.MF");
        Assert.assertTrue(Files.exists(targetManifestPath));

        final String manifestContent = new String(Files.readAllBytes(targetManifestPath));
        Assert.assertTrue(manifestContent.contains("Bundle-Vendor: Vendor name Example"));
    }
}
