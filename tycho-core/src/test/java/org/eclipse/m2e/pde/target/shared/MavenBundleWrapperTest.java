/*******************************************************************************
 * Copyright (c) 2025, 2026 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link MavenBundleWrapper} functionality.
 */
public class MavenBundleWrapperTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetEclipseSourceBundle_createsNewBundle() throws Exception {
        // Create a source JAR file
        File sourceFile = temporaryFolder.newFile("test-source.jar");
        createSourceJar(sourceFile);

        Manifest manifest = new Manifest();
        File result = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest, "com.example.bundle", "1.0.0");

        assertNotNull(result);
        assertTrue("Eclipse source bundle should exist", result.exists());
        assertEquals("com.example.bundle.source_1.0.0.jar", result.getName());
        assertEquals(sourceFile.getParentFile(), result.getParentFile());

        // Verify manifest headers
        try (JarFile jar = new JarFile(result)) {
            Manifest resultManifest = jar.getManifest();
            assertNotNull(resultManifest);
            Attributes attrs = resultManifest.getMainAttributes();
            assertEquals("com.example.bundle.source", attrs.getValue("Bundle-SymbolicName"));
            assertEquals("1.0.0", attrs.getValue("Bundle-Version"));
            assertTrue(attrs.getValue("Eclipse-SourceBundle").contains("com.example.bundle"));
        }
    }

    @Test
    public void testGetEclipseSourceBundle_returnsCache() throws Exception {
        // Create a source JAR file
        File sourceFile = temporaryFolder.newFile("cached-source.jar");
        createSourceJar(sourceFile);

        Manifest manifest1 = new Manifest();
        File result1 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest1, "com.example.bundle", "1.0.0");

        // Remember modification time
        long firstModTime = result1.lastModified();

        // Wait a bit to ensure timestamp would be different if regenerated
        Thread.sleep(100);

        // Call again - should return cached version
        Manifest manifest2 = new Manifest();
        File result2 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest2, "com.example.bundle", "1.0.0");

        assertEquals(result1.getAbsolutePath(), result2.getAbsolutePath());
        assertEquals("Cache should be reused, modification time should be unchanged", firstModTime,
                result2.lastModified());
    }

    @Test
    public void testGetEclipseSourceBundle_regeneratesWhenSourceChanged() throws Exception {
        // Create a source JAR file
        File sourceFile = temporaryFolder.newFile("changing-source.jar");
        createSourceJar(sourceFile);

        Manifest manifest1 = new Manifest();
        File result1 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest1, "com.example.bundle", "1.0.0");
        long firstSize = result1.length();

        // Modify the source file
        Thread.sleep(100); // Ensure different timestamp
        createSourceJar(sourceFile, "additional content");

        // Update source file timestamp to be newer
        sourceFile.setLastModified(System.currentTimeMillis());

        Manifest manifest2 = new Manifest();
        File result2 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest2, "com.example.bundle", "1.0.0");

        assertEquals(result1.getAbsolutePath(), result2.getAbsolutePath());
        // The file should be regenerated (different size due to different content)
        assertTrue("Regenerated bundle should have different size", result2.length() != firstSize);
    }

    @Test
    public void testGetEclipseSourceBundle_differentBSNCreatesSeparateCache() throws Exception {
        // Create a source JAR file
        File sourceFile = temporaryFolder.newFile("artifact-source.jar");
        createSourceJar(sourceFile);

        // Wrap the same source with different BSN/version
        Manifest manifest1 = new Manifest();
        File result1 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest1, "com.example.bundle", "1.0.0");

        Manifest manifest2 = new Manifest();
        File result2 = MavenBundleWrapper.getEclipseSourceBundle(sourceFile, manifest2, "com.other.bundle", "2.0.0");

        // Should be different files
        assertFalse("Different BSN/version should produce different cache files",
                result1.getAbsolutePath().equals(result2.getAbsolutePath()));
        assertEquals("com.example.bundle.source_1.0.0.jar", result1.getName());
        assertEquals("com.other.bundle.source_2.0.0.jar", result2.getName());

        // Verify each has correct manifest
        try (JarFile jar1 = new JarFile(result1)) {
            Attributes attrs1 = jar1.getManifest().getMainAttributes();
            assertEquals("com.example.bundle.source", attrs1.getValue("Bundle-SymbolicName"));
            assertEquals("1.0.0", attrs1.getValue("Bundle-Version"));
        }
        try (JarFile jar2 = new JarFile(result2)) {
            Attributes attrs2 = jar2.getManifest().getMainAttributes();
            assertEquals("com.other.bundle.source", attrs2.getValue("Bundle-SymbolicName"));
            assertEquals("2.0.0", attrs2.getValue("Bundle-Version"));
        }
    }

    @Test
    public void testIsOutdated_returnsTrueForMissingCache() throws Exception {
        File sourceFile = temporaryFolder.newFile("source.jar");
        File cacheFile = new File(temporaryFolder.getRoot(), "cache.jar");

        assertTrue("Missing cache file should be outdated",
                MavenBundleWrapper.isOutdated(cacheFile.toPath(), sourceFile.toPath()));
    }

    @Test
    public void testIsOutdated_returnsFalseForUpToDateCache() throws Exception {
        File sourceFile = temporaryFolder.newFile("source.jar");
        File cacheFile = temporaryFolder.newFile("cache.jar");

        // Set cache to be same timestamp as source
        cacheFile.setLastModified(sourceFile.lastModified());

        assertFalse("Cache with same timestamp should not be outdated",
                MavenBundleWrapper.isOutdated(cacheFile.toPath(), sourceFile.toPath()));
    }

    @Test
    public void testIsOutdated_returnsTrueWhenSourceNewer() throws Exception {
        File sourceFile = temporaryFolder.newFile("source.jar");
        File cacheFile = temporaryFolder.newFile("cache.jar");

        // Set source to be newer than cache
        Thread.sleep(100);
        sourceFile.setLastModified(System.currentTimeMillis());
        cacheFile.setLastModified(sourceFile.lastModified() - 1000);

        assertTrue("Cache should be outdated when source is newer",
                MavenBundleWrapper.isOutdated(cacheFile.toPath(), sourceFile.toPath()));
    }

    private void createSourceJar(File file) throws IOException {
        createSourceJar(file, null);
    }

    private void createSourceJar(File file, String additionalContent) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), manifest)) {
            // Add a dummy source file
            jos.putNextEntry(new ZipEntry("com/example/Test.java"));
            String content = "package com.example;\npublic class Test {}";
            if (additionalContent != null) {
                content += "\n// " + additionalContent;
            }
            jos.write(content.getBytes());
            jos.closeEntry();
        }
    }
}
