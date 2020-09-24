/*******************************************************************************
 * Copyright (c) 2014, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Alexander Nyßen (itemis AG) - Fix for bug #482469
 *******************************************************************************/
package org.eclipse.tycho.plugins.tar;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TarGzArchiverTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private TarGzArchiver archiver;

    private File tarGzArchive;
    private File archiveRoot;
    private File testPermissionsFile;
    private File testOwnerAndGroupFile;

    @Before
    public void createTestFiles() throws Exception {
        archiver = new TarGzArchiver();
        tarGzArchive = tempFolder.newFile("test.tar.gz");
        archiver.setDestFile(tarGzArchive);
        this.archiveRoot = tempFolder.newFolder("dir1");
        File dir2 = new File(archiveRoot, "dir2");
        assertTrue(dir2.mkdirs());
        archiver.addDirectory(archiveRoot);
        File textFile = new File(dir2, "test.txt");
        assertTrue(textFile.createNewFile());
        Files.writeString(textFile.toPath(), "hello");
        File dir3 = new File(dir2, "dir3");
        assertTrue(dir3.mkdirs());
        assertTrue(new File(dir3, "test.sh").createNewFile());
        this.testPermissionsFile = new File(dir2, "testPermissions");
        assertTrue(testPermissionsFile.createNewFile());
        File testLastModifiedFile = new File(dir2, "testLastModified");
        assertTrue(testLastModifiedFile.createNewFile());
        testLastModifiedFile.setLastModified(0L);
        this.testOwnerAndGroupFile = new File(dir2, "testOwnerAndGroupName");
        assertTrue(testOwnerAndGroupFile.createNewFile());
    }

    @Test
    public void testCreateArchiveEntriesPresent() throws Exception {
        archiver.createArchive();
        Map<String, TarArchiveEntry> tarEntries = getTarEntries();
        assertEquals(7, tarEntries.size());
        assertThat(tarEntries.keySet(), hasItems("dir2/", "dir2/test.txt", "dir2/dir3/", "dir2/dir3/test.sh",
                "dir2/testPermissions", "dir2/testLastModified", "dir2/testOwnerAndGroupName"));
        TarArchiveEntry dirArchiveEntry = tarEntries.get("dir2/");
        assertTrue(dirArchiveEntry.isDirectory());
        TarArchiveEntry textFileEntry = tarEntries.get("dir2/test.txt");
        assertTrue(textFileEntry.isFile());
        byte[] content = getTarEntry("dir2/test.txt");
        assertEquals("hello", new String(content, StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateArchiveEntriesLastModifiedPreserved() throws Exception {
        archiver.createArchive();
        Map<String, TarArchiveEntry> tarEntries = getTarEntries();
        assertEquals(new Date(0L), tarEntries.get("dir2/testLastModified").getModTime());
    }

    @Test
    public void testCreateArchivePermissionsPreserved() throws Exception {
        setPermissionsTo700();
        archiver.createArchive();
        assertEquals(0700, getTarEntries().get("dir2/testPermissions").getMode());
    }

    @Test
    public void testSymbolicLinkWithinArchivePreserved() throws Exception {
        createSymbolicLink(new File(archiveRoot, "testSymLink"), Paths.get("dir2/dir3", "test.sh"));
        archiver.createArchive();
        TarArchiveEntry symLinkEntry = getTarEntries().get("testSymLink");
        assertTrue(symLinkEntry.isSymbolicLink());
        assertEquals("dir2/dir3/test.sh", symLinkEntry.getLinkName());
    }

    @Test
    public void testRelativeSymbolicLinkWithinArchivePreserved() throws Exception {
        createSymbolicLink(new File(archiveRoot, "dir2/testSymLink"), Paths.get("../", "test.sh"));
        archiver.createArchive();
        TarArchiveEntry symLinkEntry = getTarEntries().get("dir2/testSymLink");
        assertTrue(symLinkEntry.isSymbolicLink());
        assertEquals("../test.sh", symLinkEntry.getLinkName());
    }

    @Test
    public void testRelativeSymbolicLinkToFolderWithinArchivePreserved() throws Exception {
        createSymbolicLink(new File(archiveRoot, "dir2/testSymLink"), Paths.get("../"));
        archiver.createArchive();
        TarArchiveEntry symLinkEntry = getTarEntries().get("dir2/testSymLink");
        assertTrue(symLinkEntry.isSymbolicLink());
        assertEquals("..", symLinkEntry.getLinkName());
        assertEquals("Expect 8 entries in the archive", 8, getTarEntries().size());
    }

    @Test
    public void testSymbolicLinkAbsoluteTargetConvertedToRelative() throws Exception {
        // use absolute path as symlink target
        Path absoluteLinkTarget = new File(archiveRoot, "dir2/dir3/test.sh").getAbsoluteFile().toPath();
        createSymbolicLink(new File(archiveRoot, "dir2/testSymLink"), absoluteLinkTarget);
        archiver.createArchive();
        TarArchiveEntry symLinkEntry = getTarEntries().get("dir2/testSymLink");
        assertTrue(symLinkEntry.isSymbolicLink());
        final String relativeLinkTarget = "dir3/test.sh";
        assertEquals(relativeLinkTarget, symLinkEntry.getLinkName());
    }

    @Test
    public void testSymbolicLinkOutsideArchiveInlined() throws Exception {
        File linkTargetFile = tempFolder.newFile("linkTargetOutsideArchiveRoot");
        Files.writeString(linkTargetFile.toPath(), "testContent");
        createSymbolicLink(new File(archiveRoot, "testSymLink"), linkTargetFile.toPath());
        archiver.createArchive();
        TarArchiveEntry inlinedSymLinkEntry = getTarEntries().get("testSymLink");
        assertFalse(inlinedSymLinkEntry.isSymbolicLink());
        assertTrue(inlinedSymLinkEntry.isFile());
        String content = new String(getTarEntry("testSymLink"), StandardCharsets.UTF_8);
        assertEquals("testContent", content);
    }

    @Test
    public void testSymbolicLinkToDirOutsideArchiveInlined() throws Exception {
        File linkTargetDir = tempFolder.newFolder("dirLinkTargetOutsideArchiveRoot");
        Files.writeString(new File(linkTargetDir, "test.txt").toPath(), "testContent");
        createSymbolicLink(new File(archiveRoot, "testDirSymLink"), linkTargetDir.toPath());
        archiver.createArchive();
        TarArchiveEntry inlinedSymLinkEntry = getTarEntries().get("testDirSymLink/");
        assertFalse(inlinedSymLinkEntry.isSymbolicLink());
        assertTrue(inlinedSymLinkEntry.isDirectory());
        String content = new String(getTarEntry("testDirSymLink/test.txt"), StandardCharsets.UTF_8);
        assertEquals("testContent", content);
    }

    @Test
    public void testLongPathEntry() throws Exception {
        final String longPath = "very/long/path/exceeding/100/chars/very/long/path/exceeding/100/chars/very/long/path/exceeding/100/chars/test.txt";
        File longPathFile = new File(archiveRoot, longPath);
        assertTrue(longPathFile.getParentFile().mkdirs());
        assertTrue(longPathFile.createNewFile());
        archiver.createArchive();
        assertTrue(getTarEntries().containsKey(longPath));
    }

    private void setPermissionsTo700() {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(testPermissionsFile.toPath(), perms);
        } catch (Exception e) {
            Assume.assumeNoException("skip test on filesystems that do not support POSIX file permissions", e);
        }
    }

    private void createSymbolicLink(File link, Path linkTarget) {
        try {
            Files.createSymbolicLink(link.toPath(), linkTarget);
        } catch (Exception e) {
            Assume.assumeNoException("skip test on filesystems that do not support symbolic links", e);
        }
    }

    private Map<String, TarArchiveEntry> getTarEntries() throws IOException, FileNotFoundException {
        TarArchiveInputStream tarStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(tarGzArchive)));
        Map<String, TarArchiveEntry> entries = new HashMap<>();
        try {
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tarStream.getNextTarEntry()) != null) {
                entries.put(tarEntry.getName(), tarEntry);
            }
        } finally {
            tarStream.close();
        }
        return entries;
    }

    private byte[] getTarEntry(String name) throws IOException {
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(tarGzArchive)))) {
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tarStream.getNextTarEntry()) != null) {
                if (name.equals(tarEntry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(tarStream, baos);
                    return baos.toByteArray();
                }
            }
        }
        throw new IOException(name + " not found in " + tarGzArchive);
    }
}
