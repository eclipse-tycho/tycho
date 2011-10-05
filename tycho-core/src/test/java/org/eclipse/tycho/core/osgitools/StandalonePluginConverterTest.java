package org.eclipse.tycho.core.osgitools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleException;

public class StandalonePluginConverterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private PluginConverter converter;

    @Before
    public void setup() {
        converter = new StandalonePluginConverter();
    }

    @Test
    public void testConvertPre30Manifest() throws PluginConversionException, FileNotFoundException, BundleException {
        File mf = new File(folder.getRoot(), "MANIFEST");
        converter.convertManifest(new File("src/test/resources/targetplatforms/pre-3.0/plugins/testjar_1.0.0.jar"), mf,
                false, "3.2", true, null);
        Assert.assertTrue(mf.isFile());
        Headers<String, String> headers = Headers.parseManifest(new FileInputStream(mf));
        Assert.assertEquals("testjar", headers.get("Bundle-SymbolicName"));
    }

    @Test
    public void testWriteManifest() throws PluginConversionException, BundleException, IOException {
        File tmpManifestFile = folder.newFile("testManifest");
        Hashtable<String, String> manifestToWrite = new Hashtable<String, String>();
        Headers<String, String> originalManifest = Headers.parseManifest(getClass().getResourceAsStream(
                "/manifests/valid.mf"));
        for (Enumeration<String> keys = originalManifest.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            manifestToWrite.put(key, originalManifest.get(key));
        }
        converter.writeManifest(tmpManifestFile, manifestToWrite, false);
        Headers<String, String> writtenManifest = Headers.parseManifest(new FileInputStream(tmpManifestFile));
        assertEquals(originalManifest.size(), writtenManifest.size());
        for (Enumeration<String> keys = writtenManifest.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            assertEquals(originalManifest.get(key), writtenManifest.get(key));
        }
    }

}
