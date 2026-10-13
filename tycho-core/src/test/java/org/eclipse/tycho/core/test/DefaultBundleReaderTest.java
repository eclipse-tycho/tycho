/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.inject.Inject;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TychoPlexusExtension.class)
public class DefaultBundleReaderTest {

    @Inject
    private BundleReader bundleReader;

    private File cacheDir;

    @BeforeEach
    public void setUp() throws Exception {
        cacheDir = File.createTempFile("cache", "");
        cacheDir.delete();
        cacheDir.mkdirs();
        ((DefaultBundleReader) bundleReader).setCacheLocation(cacheDir);
    }

    @AfterEach
    public void cleanup() throws Exception {
        FileUtils.deleteDirectory(cacheDir);
    }

    @Test
    public void testExtractDirClasspathEntries() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File libDirectory = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib/");
        assertTrue(libDirectory.isDirectory(), "directory classpath entry lib/ not extracted");
        assertTrue(new File(libDirectory, "log4j.properties").isFile());
        assertTrue(new File(libDirectory, "subdir/test.txt").isFile());
    }

    @Test
    public void testEntryMissingTrailingSlash() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File libDirectory = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib");
        assertTrue(libDirectory.isDirectory(), "directory classpath entry lib/ not extracted");
        assertTrue(new File(libDirectory, "log4j.properties").isFile());
        assertTrue(new File(libDirectory, "subdir/test.txt").isFile());
    }

    @Test
    public void testExtractSingleFile() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File singleFile = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib/log4j.properties");
        assertTrue(singleFile.isFile());
        assertFalse(new File(singleFile.getParentFile(), "subdir").exists());
        assertFalse(new File(singleFile.getParentFile().getParentFile(), "META-INF").exists());
    }

    @Test
    public void testNonExistingEntry() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File nonExistingFile = bundleReader.getEntry(bundleWithNestedDirClasspath, "foo/bar.txt");
        assertNull(nonExistingFile);
    }

    @Test
    public void testGetEntryExtractionCache() throws Exception {
        File bundleJar = getTestJar();
        File extractedLog4jFile = bundleReader.getEntry(bundleJar, "lib/log4j.properties");
        assertTrue(extractedLog4jFile.isFile());
        long firstExtractionTimestamp = extractedLog4jFile.lastModified();
        File extractedDir = bundleReader.getEntry(bundleJar, "lib/");
        // make sure subdirectory of lib/ is extracted even if directory lib/ is already extracted
        // due to previous extraction of "lib/log4j.properties"
        assertTrue(new File(extractedDir, "subdir").isDirectory());
        // extract the same file with older timestamp in archive again => must be a cache hit (skip extraction)
        File olderContentBundleJar = new File(getBasedir(),
                "src/test/resources/bundlereader/olderTimestamp/testNestedDirClasspath_1.0.0.201007261122.jar");
        File log4jFileExtractedAgain = bundleReader.getEntry(olderContentBundleJar, "lib/log4j.properties");
        assertEquals(log4jFileExtractedAgain.getCanonicalPath(), extractedLog4jFile.getCanonicalPath());
        assertEquals(firstExtractionTimestamp, log4jFileExtractedAgain.lastModified());
    }

    @Test
    public void testGetEntryExternalJar() throws Exception {
        File bundleJar = getTestJar();
        // 370958 IOException will only occur if extraction dir exists already
        new File(cacheDir, bundleJar.getName()).mkdirs();
        File externalLib = bundleReader.getEntry(bundleJar, "external:$user.home$/external-lib.jar");
        assertNull(externalLib);
    }

    @Test
    public void testLoadManifestFromDir() throws Exception {
        File dir = new File("src/test/resources/bundlereader/dirshape");
        OsgiManifest manifest = bundleReader.loadManifest(dir);
        assertEquals("org.eclipse.tycho.test", manifest.getBundleSymbolicName());
    }

    @Test
    public void testLoadManifestFromJar() throws Exception {
        File jar = new File("src/test/resources/bundlereader/jarshape/test.jar");
        OsgiManifest manifest = bundleReader.loadManifest(jar);
        assertEquals("org.eclipse.tycho.test", manifest.getBundleSymbolicName());
    }

    @Test
    public void testLoadManifestFromInvalidDir() throws Exception {
        // dir has no META-INF/MANIFEST.MF nor plugin.xml/fragment.xml
        File dir = new File("src/test/resources/bundlereader/invalid");
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> bundleReader.loadManifest(dir));
        assertTrue(e.getMessage().contains("Manifest file not found"));
    }

    @Test
    public void testLoadManifestFromCorruptedJar() throws Exception {
        File jar = new File("src/test/resources/bundlereader/invalid/corrupt.jar");
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> bundleReader.loadManifest(jar));
        if (System.getProperty("java.specification.version").compareTo("11") >= 0) {
            assertTrue(e.getMessage().contains("zip END header not found"));
        } else {
            assertTrue(e.getMessage().contains("error in opening zip file"));
        }
    }

    @Test
    public void testLoadManifestFromNonexistingFile() throws Exception {
        File jar = new File("NON_EXISTING.jar");
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> bundleReader.loadManifest(jar));
        assertTrue(e.getMessage().contains("Manifest file not found"));
    }

    @Test
    public void testLoadManifestFromPlainJar() throws Exception {
        File plainJar = new File("src/test/resources/bundlereader/jarshape/plain.jar");
        OsgiManifestParserException e = assertThrows(OsgiManifestParserException.class,
                () -> bundleReader.loadManifest(plainJar));
        assertTrue(e.getMessage().contains("Exception parsing OSGi MANIFEST"));
        assertTrue(e.getMessage().contains("is missing"));
    }

    private File getTestJar() {
        return new File(getBasedir(), "src/test/resources/bundlereader/testNestedDirClasspath_1.0.0.201007261122.jar");
    }
}
