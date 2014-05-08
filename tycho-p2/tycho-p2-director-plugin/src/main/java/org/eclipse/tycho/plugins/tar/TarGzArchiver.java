/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
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
 * <li>Owner name and group name</li>
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
    private List<File> sourceDirs = new ArrayList<File>();
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
            GzipCompressorOutputStream gzipStream = new GzipCompressorOutputStream(new BufferedOutputStream(
                    new FileOutputStream(destFile)));
            tarStream = new TarArchiveOutputStream(gzipStream, "UTF-8");
            // allow "long" file paths (> 100 chars)
            tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
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

    private void addToTarRecursively(File tarRootDir, File source, TarArchiveOutputStream tarStream) throws IOException {
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

    private TarArchiveEntry createTarEntry(File tarRootDir, File source) throws IOException {
        String pathInTar = slashify(tarRootDir.toPath().relativize(source.toPath()));
        log.debug("Adding entry " + pathInTar);
        TarArchiveEntry tarEntry;
        if (isSymbolicLink(source) && resolvesBelow(source.toPath(), tarRootDir.toPath())) {
            // only create symlink entry if link target is inside archive
            tarEntry = new TarArchiveEntry(pathInTar, TarArchiveEntry.LF_SYMLINK);
            tarEntry.setLinkName(slashify(getRelativeSymLinkTarget(source.toPath(), tarRootDir.toPath())));
        } else {
            tarEntry = new TarArchiveEntry(source, pathInTar);
        }
        PosixFileAttributes attrs = getAttributes(source);
        if (attrs != null) {
            tarEntry.setUserName(attrs.owner().getName());
            tarEntry.setGroupName(attrs.group().getName());
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
        BufferedInputStream sourceStream = new BufferedInputStream(new FileInputStream(source));
        try {
            IOUtils.copy(sourceStream, tarStream);
        } finally {
            sourceStream.close();
        }
    }

    private boolean resolvesBelow(Path source, Path baseDir) throws IOException {
        return ! resolvedLinkTargetIsOutside(resolveLinkTarget(source), baseDir);
    }

    private Path getRelativeSymLinkTarget(Path source, Path baseDir) throws IOException {
        // link target may be relative, so we resolve it first
        Path resolvedLinkTarget = resolveLinkTarget(source);
        if (resolvedLinkTargetIsOutside(resolvedLinkTarget, baseDir)) {
       	    Path relative = baseDir.relativize(resolvedLinkTarget);
            log.debug("Detected symlink target outside of tar file: " + slashify(relative) + " for " + source);
	    return relative;
        }
        return resolveRelativeLinkTarget(source);
    }

    private Path resolveLinkTarget(Path source) throws IOException {
        Path linkTarget = resolveRelativeLinkTarget(source);
        return source.getParent().resolve(linkTarget);
    }

    private Path resolveRelativeLinkTarget(Path source) throws IOException {
        return Files.readSymbolicLink(source);
    }

    private boolean resolvedLinkTargetIsOutside(Path resolved, Path baseDir) {
        return ! resolved.startsWith(baseDir);
    }

    private static boolean isSymbolicLink(File file) {
        return Files.isSymbolicLink(file.toPath());
    }

}
