/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class DefaultBundleReaderTest extends AbstractTychoMojoTestCase {

    private File cacheDir;

    private DefaultBundleReader bundleReader;

    @Override
    protected void setUp() throws Exception {
        cacheDir = File.createTempFile("cache", "");
        cacheDir.delete();
        cacheDir.mkdirs();
        bundleReader = (DefaultBundleReader) lookup(BundleReader.class);
        bundleReader.setLocationRepository(cacheDir);
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(cacheDir);
    }

    public void testExtractDirClasspathEntries() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File libDirectory = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib/");
        assertTrue("directory classpath entry lib/ not extracted", libDirectory.isDirectory());
        assertTrue(new File(libDirectory, "log4j.properties").isFile());
        assertTrue(new File(libDirectory, "subdir/test.txt").isFile());
    }

    public void testEntryMissingTrailingSlash() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File libDirectory = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib");
        assertTrue("directory classpath entry lib/ not extracted", libDirectory.isDirectory());
        assertTrue(new File(libDirectory, "log4j.properties").isFile());
        assertTrue(new File(libDirectory, "subdir/test.txt").isFile());
    }

    public void testExtractSingleFile() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File singleFile = bundleReader.getEntry(bundleWithNestedDirClasspath, "lib/log4j.properties");
        assertTrue(singleFile.isFile());
        assertFalse(new File(singleFile.getParentFile(), "subdir").exists());
        assertFalse(new File(singleFile.getParentFile().getParentFile(), "META-INF").exists());
    }

    public void testNonExistingEntry() throws Exception {
        File bundleWithNestedDirClasspath = getTestJar();
        File nonExistingFile = bundleReader.getEntry(bundleWithNestedDirClasspath, "foo/bar.txt");
        assertNull(nonExistingFile);
    }

    private File getTestJar() {
        return new File(getBasedir(), "src/test/resources/bundlereader/testNestedDirClasspath_1.0.0.201007261122.jar");
    }
}
