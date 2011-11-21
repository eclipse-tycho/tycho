package org.eclipse.tycho.core.osgitools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URISyntaxException;

import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.junit.Test;

public class OsgiManifestTest {

    @Test
    public void testValidOSGiManifest() throws OsgiManifestParserException, URISyntaxException {
        OsgiManifest manifest = parseManifest("valid.mf");
        assertEquals("org.eclipse.tycho.test", manifest.getBundleSymbolicName());
        assertEquals("0.1.0.qualifier", manifest.getBundleVersion());
        assertArrayEquals(new String[] { "." }, manifest.getBundleClasspath());
        assertEquals(13, manifest.getHeaders().size());
    }

    @Test(expected = OsgiManifestParserException.class)
    public void testInvalidManifest() throws OsgiManifestParserException, URISyntaxException {
        parseManifest("invalid.mf");
    }

    @Test
    public void testMissingSymbolicName() throws Exception {
        try {
            parseManifest("noBsn.mf");
            fail();
        } catch (InvalidOSGiManifestException e) {
            assertEquals(
                    "Exception parsing OSGi MANIFEST testLocation: MANIFEST header 'Bundle-SymbolicName' not found",
                    e.getMessage());
        }
    }

    @Test
    public void testMissingVersion() throws Exception {
        try {
            parseManifest("noVersion.mf");
            fail();
        } catch (InvalidOSGiManifestException e) {
            assertEquals("Exception parsing OSGi MANIFEST testLocation: MANIFEST header 'Bundle-Version' not found",
                    e.getMessage());
        }
    }

    @Test
    public void testInvalidVersion() throws Exception {
        try {
            parseManifest("invalidVersion.mf");
            fail();
        } catch (InvalidOSGiManifestException e) {
            assertEquals("Exception parsing OSGi MANIFEST testLocation: invalid qualifier: %invalidQualifier",
                    e.getMessage());
        }
    }

    @Test
    public void testInvalidVersionQualifier() throws Exception {
        try {
            parseManifest("invalidVersionQualifier.mf");
            fail();
        } catch (InvalidOSGiManifestException e) {
            assertEquals("Exception parsing OSGi MANIFEST testLocation: Bundle-Version 'invalid' is invalid",
                    e.getMessage());
        }
    }

    @Test
    public void testBundleClasspath() throws OsgiManifestParserException, URISyntaxException {
        OsgiManifest manifest = parseManifest("classpath.mf");
        assertArrayEquals(new String[] { "lib/foo.jar", "dir/", "baz.jar" }, manifest.getBundleClasspath());
    }

    @Test
    public void testDirectoryShape() throws OsgiManifestParserException, URISyntaxException {
        OsgiManifest manifest = parseManifest("dirShape.mf");
        assertEquals(true, manifest.isDirectoryShape());
    }

    @Test
    public void testManifestAttributesAreNonCaseSensitive() throws OsgiManifestParserException, URISyntaxException {
        OsgiManifest manifest = parseManifest("valid.mf");
        assertEquals("0.1.0.qualifier", manifest.getHeaders().get("bUNDLE-vERSION"));
    }

    @Test
    public void testMultipleBREEs() throws Exception {
        OsgiManifest manifest = parseManifest("bree.mf");
        ExecutionEnvironment[] expected = { ExecutionEnvironmentUtils.getExecutionEnvironment("J2SE-1.5"),
                ExecutionEnvironmentUtils.getExecutionEnvironment("JavaSE-1.7") };
        assertArrayEquals(expected, manifest.getExecutionEnvironments());
    }

    @Test
    public void testNoBREE() throws Exception {
        OsgiManifest manifest = parseManifest("noBree.mf");
        ExecutionEnvironment[] expected = new ExecutionEnvironment[0];
        assertArrayEquals(expected, manifest.getExecutionEnvironments());
    }

    @Test(expected = OsgiManifestParserException.class)
    public void testInvalidBREE() throws Exception {
        parseManifest("invalidBree.mf");
    }

    private OsgiManifest parseManifest(String manifestName) throws URISyntaxException {
        InputStream stream = getClass().getResourceAsStream("/manifests/" + manifestName);
        return OsgiManifest.parse(stream, "testLocation");
    }
}
