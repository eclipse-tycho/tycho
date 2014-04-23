package org.eclipse.tycho.plugins.tar;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
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
import org.codehaus.plexus.util.FileUtils;
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

    private File testPermissionsFile;
    private File testOwnerAndGroupFile;

    private File archiveRoot;

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
        FileUtils.fileWrite(textFile, "hello");
        assertTrue(new File(dir2, "test.sh").createNewFile());
        this.testPermissionsFile = new File(dir2, "testPermissions");
        assertTrue(testPermissionsFile.createNewFile());
        File testLastModifiedFile = new File(dir2, "testLastModified");
        assertTrue(testLastModifiedFile.createNewFile());
        testLastModifiedFile.setLastModified(0L);
        this.testOwnerAndGroupFile = new File(dir2, "testOwnerAndGroupName");
        assertTrue(testOwnerAndGroupFile.createNewFile());
    }

    private void setPermissionsTo700() {
        try {
            Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(testPermissionsFile.toPath(), perms);
        } catch (Exception e) {
            Assume.assumeNoException("skip test on filesystems that do not support POSIX file permissions", e);
        }
    }

    private PosixFileAttributes getPosixFileAttributes(File file) {
        try {
            PosixFileAttributeView attributeView = Files.getFileAttributeView(file.toPath(),
                    PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            return attributeView.readAttributes();
        } catch (Exception e) {
            Assume.assumeNoException("skip test on filesystems that do not support POSIX file attributes", e);
        }
        // never reached
        return null;
    }

    private void createSymbolicLink(File link, Path linkTarget) {
        try {
            Files.createSymbolicLink(link.toPath(), linkTarget);
        } catch (Exception e) {
            Assume.assumeNoException("skip test on filesystems that do not support symbolic links", e);
        }
    }

    @Test
    public void testCreateArchiveEntriesPresent() throws Exception {
        archiver.createArchive();
        Map<String, TarArchiveEntry> tarEntries = getTarEntries();
        assertEquals(6, tarEntries.size());
        assertThat(tarEntries.keySet(),
                hasItems("dir2/", "dir2/test.txt", "dir2/test.sh", "dir2/testPermissions", "dir2/testLastModified"));
        TarArchiveEntry dirArchiveEntry = tarEntries.get("dir2/");
        assertTrue(dirArchiveEntry.isDirectory());
        TarArchiveEntry textFileEntry = tarEntries.get("dir2/test.txt");
        assertTrue(textFileEntry.isFile());
        byte[] content = getTarEntry("dir2/test.txt");
        assertEquals("hello", new String(content, "UTF-8"));
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
        createSymbolicLink(new File(archiveRoot, "testSymLink"), Paths.get("dir2", "test.sh"));
        archiver.createArchive();
        TarArchiveEntry symLinkEntry = getTarEntries().get("testSymLink");
        assertTrue(symLinkEntry.isSymbolicLink());
        assertEquals("dir2/test.sh", symLinkEntry.getLinkName());
    }

    @Test
    public void testSymbolicLinkOutsideArchiveInlined() throws Exception {
        File linkTargetFile = tempFolder.newFile("linkTargetOutsideArchiveRoot");
        FileUtils.fileWrite(linkTargetFile, "testContent");
        createSymbolicLink(new File(archiveRoot, "testSymLink"), linkTargetFile.toPath());
        archiver.createArchive();
        TarArchiveEntry inlinedSymLinkEntry = getTarEntries().get("testSymLink");
        assertFalse(inlinedSymLinkEntry.isSymbolicLink());
        assertTrue(inlinedSymLinkEntry.isFile());
        String content = new String(getTarEntry("testSymLink"), "UTF-8");
        assertEquals("testContent", content);
    }

    @Test
    public void testCreateArchiveOwnerAndGroupPreserved() throws Exception {
        PosixFileAttributes attrs = getPosixFileAttributes(testOwnerAndGroupFile);
        archiver.createArchive();
        TarArchiveEntry testOwnerAndGroupNameEntry = getTarEntries().get("dir2/testOwnerAndGroupName");
        assertEquals(attrs.owner().getName(), testOwnerAndGroupNameEntry.getUserName());
        assertEquals(attrs.group().getName(), testOwnerAndGroupNameEntry.getGroupName());
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

    private Map<String, TarArchiveEntry> getTarEntries() throws IOException, FileNotFoundException {
        TarArchiveInputStream tarStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(
                tarGzArchive)));
        Map<String, TarArchiveEntry> entries = new HashMap<String, TarArchiveEntry>();
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
        TarArchiveInputStream tarStream = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(
                tarGzArchive)));
        try {
            TarArchiveEntry tarEntry = null;
            while ((tarEntry = tarStream.getNextTarEntry()) != null) {
                if (name.equals(tarEntry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(tarStream, baos);
                    return baos.toByteArray();
                }
            }
        } finally {
            tarStream.close();
        }
        throw new IOException(name + " not found in " + tarGzArchive);
    }
}
