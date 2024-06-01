package org.eclipse.tycho.core.osgitools;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import java.net.URISyntaxException;

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

    @Test
    public void testInvalidManifest() throws OsgiManifestParserException, URISyntaxException {
        assertThrows(OsgiManifestParserException.class, () -> parseManifest("invalid.mf"));
    }

    @Test
    public void testMissingSymbolicName() throws Exception {
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> parseManifest("noBsn.mf"));
        assertThat(e.getMessage(), containsString("Bundle-SymbolicName header is required"));

    }

    @Test
    public void testMissingVersion() throws Exception {
        assertEquals("0.0.0", parseManifest("noVersion.mf").getBundleVersion());
    }

    @Test
    public void testInvalidVersion() throws Exception {
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> parseManifest("invalidVersion.mf"));
        assertThat(e.getMessage(),
                containsString("Invalid Manifest header \"Bundle-Version\": 1.0.0.%invalidQualifier"));
    }

    @Test
    public void testDuplicateImport() throws Exception {
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> parseManifest("duplicateImport.mf"));
        assertThat(e.getMessage(), containsString(
                "Invalid manifest header Import-Package: \"org.w3c.dom\" : Cannot import a package more than once \"org.w3c.dom\""));
    }

    @Test
    public void testInvalidVersionQualifier() throws Exception {
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> parseManifest("invalidVersionQualifier.mf"));
        assertThat(e.getMessage(), containsString("Invalid Manifest header \"Bundle-Version\""));
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
        assertArrayEquals(new String[] { "J2SE-1.5", "JavaSE-1.7" }, manifest.getExecutionEnvironments());
    }

    @Test
    public void testNoBREE() throws Exception {
        OsgiManifest manifest = parseManifest("noBree.mf");
        assertArrayEquals(new String[0], manifest.getExecutionEnvironments());
    }

    private OsgiManifest parseManifest(String manifestName) throws URISyntaxException {
        InputStream stream = getClass().getResourceAsStream("/manifests/" + manifestName);
        return OsgiManifest.parse(stream, "testLocation");
    }
}
