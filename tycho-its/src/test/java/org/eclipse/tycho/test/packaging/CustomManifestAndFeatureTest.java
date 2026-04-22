package org.eclipse.tycho.test.packaging;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomManifestAndFeatureTest extends AbstractTychoIntegrationTest {
    public static final Pattern PATTERN_BUNDLE_VENDOR = Pattern.compile("Bundle-Vendor:(.*)");
    public static final Pattern PATTERN_FEATURE_PROVIDER = Pattern.compile("provider-name=\"(.*)\"");

    @Test
    public void testManifestAndFeatureConfigsHaveBeenFiltered() throws Exception {
        final Verifier verifier = getVerifier("/packaging.manifestAndFeature", false);
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        // Verify filtered MANIFEST.MF
        final Path targetManifestPath = Path.of(verifier.getBasedir(), "plugin1/target/MANIFEST.MF");
        Assert.assertTrue("Missing MANIFEST.MF under " + targetManifestPath.getParent(), Files.exists(targetManifestPath));

        final String manifestContent = Files.readString(targetManifestPath);
        final Matcher bundleVendorMatcher = PATTERN_BUNDLE_VENDOR.matcher(manifestContent);
        Assert.assertTrue("Could not find Bundle-Vendor in MANIFEST.MF", bundleVendorMatcher.find());
        Assert.assertEquals("Vendor name example", bundleVendorMatcher.group(1).trim());

        // Verify filtered feature.xml
        final Path targetFeaturePath = Path.of(verifier.getBasedir(), "feature1/target/feature.xml");
        Assert.assertTrue("Missing feature.xml under " + targetFeaturePath.getParent(), Files.exists(targetFeaturePath));

        final String featureContent = Files.readString(targetFeaturePath);
        final Matcher providerMatcher = PATTERN_FEATURE_PROVIDER.matcher(featureContent);
        Assert.assertTrue("Could not find provider-name=\"...\" in feature.xml", providerMatcher.find());
        Assert.assertEquals("Provider name example", providerMatcher.group(1).trim());
    }
}
