package org.eclipse.tycho.test.osgiversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;
import org.osgi.framework.Constants;

public class OsgiVersionTest extends AbstractTychoIntegrationTest {

    @Test
    public void testOsgiVersionTest() throws Exception {
        Verifier verifier = getVerifier("/osgiversion", false);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();
    }

    private void assertTargetDirContainsGeneratedSources(Verifier verifier) throws IOException {
        File[] sourceJars = new File(verifier.getBasedir(), "/target").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith("sources.jar");
            }
        });
        assertEquals(1, sourceJars.length);
        JarFile sourceJar = new JarFile(sourceJars[0]);
        try {
            assertNotNull(sourceJar.getEntry("hellotycho/Greeting.java"));
            Attributes sourceBundleHeaders = sourceJar.getManifest().getMainAttributes();
            assertEquals("OSGI-INF/l10n/bundle-src", sourceBundleHeaders.getValue(Constants.BUNDLE_LOCALIZATION));
            ZipEntry l10nPropsEntry = sourceJar.getEntry("OSGI-INF/l10n/bundle-src.properties");
            assertNotNull(l10nPropsEntry);
            Properties l10nProps = new Properties();
            InputStream propsStream = sourceJar.getInputStream(l10nPropsEntry);
            l10nProps.load(propsStream);
            assertEquals(2, l10nProps.size());
            assertEquals("Hello Tycho Bundle Source", l10nProps.getProperty("bundleName"));
            assertEquals("Hello Tycho Vendor", l10nProps.getProperty("bundleVendor"));
        } finally {
            sourceJar.close();
        }
    }

}
