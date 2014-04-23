package org.eclipse.tycho.plugins.tar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class TarGzArchiver {

    /** Default permissions bits for directories */
    public static final int DEFAULT_DIR_MODE = 040000;

    /** Default permissions bits for files */
    public static final int DEFAULT_FILE_MODE = 0100000;

    private File destFile;
    private List<File> sourceDirs = new ArrayList<File>();

    public TarGzArchiver() {
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public void addDirectory(File directory) {
        this.sourceDirs.add(directory);
    }

    public void createArchive() throws IOException {
        validate();
        TarArchiveOutputStream tarStream = null;
        try {
            destFile.getAbsoluteFile().getParentFile().mkdirs();
            GzipCompressorOutputStream gzipStream = new GzipCompressorOutputStream(new BufferedOutputStream(
                    new FileOutputStream(destFile)));
            tarStream = new TarArchiveOutputStream(gzipStream, "UTF-8");
            // allow "long" file paths (> 100 chars)
            tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tarStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            for (File sourceDir : sourceDirs) {
                for (File child : sourceDir.listFiles()) {
                    addToTarRecursively(sourceDir, child, tarStream);
                }
            }
        } finally {
            if (tarStream != null) {
                tarStream.close();
            }
        }
    }

    private void validate() throws IOException {
        for (File sourceDir : sourceDirs) {
            if (!sourceDir.isDirectory()) {
                throw new IOException(sourceDir + " is not a directory");
            }
        }
    }

    private static void addToTarRecursively(File tarRootDir, File source, TarArchiveOutputStream tarStream)
            throws IOException {
        TarArchiveEntry tarEntry = createTarEntry(tarRootDir, source);
        tarStream.putArchiveEntry(tarEntry);
        if (source.isFile() && !tarEntry.isSymbolicLink()) {
            copyFileContentToTarStream(source, tarStream);
        }
        tarStream.closeArchiveEntry();
        if (source.isDirectory()) {
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToTarRecursively(tarRootDir, child, tarStream);
                }
            }
        }
    }

    private static TarArchiveEntry createTarEntry(File tarRootDir, File source) throws IOException {
        String pathInTar = tarRootDir.toPath().relativize(source.toPath()).toString().replace(File.separatorChar, '/');
        System.out.println("Adding " + pathInTar);
        TarArchiveEntry tarEntry;
        if (isSymbolicLink(source) && resolvesBelow(source, tarRootDir)) {
            // only create symlink entry if link target is inside archive
            tarEntry = new TarArchiveEntry(pathInTar, TarArchiveEntry.LF_SYMLINK);
            tarEntry.setLinkName(getRelativeSymLinkTarget(source, tarRootDir).toString().replace(File.separatorChar,
                    '/'));
        } else {
            tarEntry = new TarArchiveEntry(source, pathInTar);
        }
        if (isExecutable(source)) {
            tarEntry.setMode(getFileMode(source));
        }
        tarEntry.setModTime(source.lastModified());
        return tarEntry;
    }

    private static int getFileMode(File source) throws IOException {
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(source.toPath(),
                PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (fileAttributeView == null) {
            return defaultFileMode(source);
        }
        PosixFileAttributes attrs;
        try {
            attrs = fileAttributeView.readAttributes();
        } catch (IOException e) {
            return defaultFileMode(source);
        }
        int baseMode = source.isFile() ? DEFAULT_FILE_MODE : DEFAULT_DIR_MODE;
        return baseMode + FilePermissionHelper.toOctalFileMode(attrs.permissions());
    }

    private static int defaultFileMode(File source) {
        if (source.isDirectory()) {
            return TarArchiveEntry.DEFAULT_DIR_MODE;
        } else {
            return TarArchiveEntry.DEFAULT_FILE_MODE;
        }
    }

    private static void copyFileContentToTarStream(File source, TarArchiveOutputStream tarStream) throws IOException {
        BufferedInputStream sourceStream = new BufferedInputStream(new FileInputStream(source));
        try {
            IOUtils.copy(sourceStream, tarStream);
        } finally {
            sourceStream.close();
        }
    }

    private static boolean resolvesBelow(File source, File baseDir) throws IOException {
        return !getRelativeSymLinkTarget(source, baseDir).startsWith("..");
    }

    private static Path getRelativeSymLinkTarget(File source, File baseDir) throws IOException {
        Path sourcePath = source.toPath();
        Path linkTarget = Files.readSymbolicLink(sourcePath);
        // link target may be relative, so we resolve it first
        Path resolvedLinkTarget = sourcePath.getParent().resolve(linkTarget);
        System.out.println("symlink " + source + " resolves to " + resolvedLinkTarget);
        System.out.println("basedir path: " + baseDir.toPath());
        Path relative = baseDir.toPath().relativize(resolvedLinkTarget);
        System.out.println("relative symlink path: " + relative);
        return relative.normalize();
    }

    private static boolean isSymbolicLink(File file) {
        return Files.isSymbolicLink(file.toPath());
    }

    private static boolean isExecutable(File file) {
        return Files.isExecutable(file.toPath());
    }

}
