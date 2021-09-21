/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Alexander Nyßen (itemis AG) - Fix for bug #482469
 *******************************************************************************/
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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Gzipped Tar archiver which preserves
 * 
 * <ul>
 * <li>POSIX file permissions</li>
 * <li>Symbolic links (if the link target points inside the archive)</li>
 * <li>Last modification timestamp</li>
 * </ul>
 * 
 * in the archive as found in the filesystem for files to be archived. It uses GNU tar format
 * extensions for archive entries with path length > 100.
 *
 */
public class TarGzArchiver {

    private File destFile;
    private List<File> sourceDirs = new ArrayList<>();
    private Log log = new SystemStreamLog();

    public TarGzArchiver() {
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public void addDirectory(File directory) {
        this.sourceDirs.add(directory);
    }

    public void createArchive() throws IOException {
        validate();
        log.info("Building tar: " + destFile);
        TarArchiveOutputStream tarStream = null;
        try {
            destFile.getAbsoluteFile().getParentFile().mkdirs();
            GzipCompressorOutputStream gzipStream = new GzipCompressorOutputStream(
                    new BufferedOutputStream(new FileOutputStream(destFile)));
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

    private void addToTarRecursively(File tarRootDir, File source, TarArchiveOutputStream tarStream)
            throws IOException {
        TarArchiveEntry tarEntry = createTarEntry(tarRootDir, source);
        tarStream.putArchiveEntry(tarEntry);
        if (source.isFile() && !tarEntry.isSymbolicLink()) {
            copyFileContentToTarStream(source, tarStream);
        }
        tarStream.closeArchiveEntry();
        if (source.isDirectory() && (!tarEntry.isSymbolicLink() || !resolvesBelow(source, tarRootDir))) {
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToTarRecursively(tarRootDir, child, tarStream);
                }
            }
        }
    }

    private TarArchiveEntry createTarEntry(File tarRootDir, File source) throws IOException {
        String pathInTar = slashify(tarRootDir.toPath().relativize(source.toPath()));
        log.debug("Adding entry " + pathInTar);
        TarArchiveEntry tarEntry;
        if (isSymbolicLink(source) && resolvesBelow(source, tarRootDir)) {
            // only create symlink entry if link target is inside archive
            tarEntry = new TarArchiveEntry(pathInTar, TarArchiveEntry.LF_SYMLINK);
            tarEntry.setLinkName(slashify(getRelativeSymLinkTarget(source, source.getParentFile())));
        } else {
            tarEntry = new TarArchiveEntry(source, pathInTar);
        }
        PosixFileAttributes attrs = getAttributes(source);
        if (attrs != null) {
            tarEntry.setMode(FilePermissionHelper.toOctalFileMode(attrs.permissions()));
        }
        tarEntry.setModTime(source.lastModified());
        return tarEntry;
    }

    private static String slashify(Path path) {
        String pathString = path.toString();
        if (File.separatorChar == '/') {
            return pathString;
        } else {
            return pathString.replace(File.separatorChar, '/');
        }
    }

    private PosixFileAttributes getAttributes(File source) {
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(source.toPath(),
                PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (fileAttributeView == null) {
            return null;
        }
        PosixFileAttributes attrs;
        try {
            attrs = fileAttributeView.readAttributes();
        } catch (IOException e) {
            return null;
        }
        return attrs;
    }

    private static void copyFileContentToTarStream(File source, TarArchiveOutputStream tarStream) throws IOException {
        try (BufferedInputStream sourceStream = new BufferedInputStream(new FileInputStream(source))) {
            IOUtils.copy(sourceStream, tarStream);
        }
    }

    private boolean resolvesBelow(File source, File baseDir) throws IOException {
        return !getRelativeSymLinkTarget(source, baseDir).startsWith("..");
    }

    private Path getRelativeSymLinkTarget(File source, File baseDir) throws IOException {
        Path sourcePath = source.toPath();
        Path linkTarget = Files.readSymbolicLink(sourcePath);
        // link target may be relative, so we resolve it first
        Path resolvedLinkTarget = sourcePath.getParent().resolve(linkTarget);
        Path relative = baseDir.toPath().relativize(resolvedLinkTarget);
        Path normalizedSymLinkPath = relative.normalize();
        log.debug("Computed symlink target path " + slashify(normalizedSymLinkPath) + " for symlink " + source
                + " relative to " + baseDir);
        return normalizedSymLinkPath;
    }

    private static boolean isSymbolicLink(File file) {
        return Files.isSymbolicLink(file.toPath());
    }

}
