/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;

@Component(role = BundleReader.class)
public class DefaultBundleReader extends AbstractLogEnabled implements BundleReader {

    public static final String CACHE_PATH = ".cache/tycho";
    private final Map<String, OsgiManifest> manifestCache = new HashMap<>();

    private File cacheDir;
    private Set<String> extractedFiles = new HashSet<>();

    @Requirement
    private FileLockService fileLockService;

    @Override
    public OsgiManifest loadManifest(File bundleLocation) {
        String locationPath = bundleLocation.getAbsolutePath();
        OsgiManifest manifest = manifestCache.get(locationPath);
        if (manifest == null) {
            manifest = doLoadManifest(bundleLocation);
            manifestCache.put(locationPath, manifest);
        }
        return manifest;
    }

    private OsgiManifest doLoadManifest(File bundleLocation) {
        try {
            if (bundleLocation.isDirectory()) {
                return loadManifestFromDirectory(bundleLocation);
            } else if (bundleLocation.isFile()) {
                return loadManifestFromFile(bundleLocation);
            } else {
                // file does not exist
                throw new OsgiManifestParserException(new File(bundleLocation, JarFile.MANIFEST_NAME).getAbsolutePath(),
                        "Manifest file not found");
            }
        } catch (IOException e) {
            throw new OsgiManifestParserException(bundleLocation.getAbsolutePath(), e);
        }
    }

    private OsgiManifest loadManifestFromFile(File bundleLocation) throws IOException {
        if (!bundleLocation.getName().toLowerCase().endsWith(".jar")) {
            // file but not a jar, assume it is MANIFEST.MF
            return loadManifestFile(bundleLocation);
        }
        try ( // it is a jar, let's see if it has OSGi bundle manifest
                ZipFile jar = new ZipFile(bundleLocation, ZipFile.OPEN_READ)) {
            ZipEntry manifestEntry = jar.getEntry(JarFile.MANIFEST_NAME);
            if (manifestEntry != null) {
                InputStream stream = jar.getInputStream(manifestEntry);
                return OsgiManifest.parse(stream, bundleLocation.getAbsolutePath() + "!/" + JarFile.MANIFEST_NAME);
            }
        }
        throw new OsgiManifestParserException(bundleLocation.getAbsolutePath(),
                "Manifest file not found in JAR archive");
    }

    private OsgiManifest loadManifestFromDirectory(File directory) throws IOException {
        File manifestFile = new File(directory, JarFile.MANIFEST_NAME);
        if (!manifestFile.isFile()) {
            throw new OsgiManifestParserException(manifestFile.getAbsolutePath(), "Manifest file not found");
        }
        return loadManifestFile(manifestFile);
    }

    private OsgiManifest loadManifestFile(File manifestFile) throws IOException, OsgiManifestParserException {
        return OsgiManifest.parse(new FileInputStream(manifestFile), manifestFile.getAbsolutePath());
    }

    public void setLocationRepository(File basedir) {
        this.cacheDir = new File(basedir, CACHE_PATH);
    }

    @Override
    public File getEntry(File bundleLocation, String path) {
        if (path.startsWith("external:")) {
            getLogger().warn("Ignoring Bundle-ClassPath entry '" + path + "' of bundle " + bundleLocation);
            return null;
        }
        final File result;
        if (bundleLocation.isDirectory()) {
            result = new File(bundleLocation, path);
        } else {
            try {
                File outputDirectory = new File(cacheDir, bundleLocation.getName());
                result = new File(outputDirectory, path);
                String resultPath = result.getCanonicalPath();
                if (extractedFiles.contains(resultPath) && result.exists()) {
                    return result;
                } else {
                    FileLocker locker = fileLockService.getFileLocker(outputDirectory);
                    locker.lock(5 * 60 * 1000L);
                    try {
                        extractZipEntries(bundleLocation, path, outputDirectory);
                    } finally {
                        locker.release();
                    }
                    extractedFiles.add(resultPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("IOException while extracting '" + path + "' from " + bundleLocation, e);
            }
        }
        if (result.exists()) {
            return result;
        } else {
            getLogger().warn("Bundle-ClassPath entry " + path + " does not exist in " + bundleLocation);
            return null;
        }
    }

    private void extractZipEntries(File bundleLocation, String path, File outputDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(bundleLocation)) {
            ZipEntry singleEntry = zip.getEntry(path);
            InputStream singleEntryStream;
            if (singleEntry != null && !singleEntry.isDirectory()
                    && (singleEntryStream = zip.getInputStream(singleEntry)) != null) {
                // fix for performance bug 367098: avoid loop if path is a single zip file entry
                copyStreamToFile(singleEntryStream, new File(outputDirectory, singleEntry.getName()),
                        singleEntry.getTime());
            } else {
                // loop over all entries and extract matching
                for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (!zipEntry.isDirectory() && zipEntry.getName().startsWith(path)) {
                        copyStreamToFile(zip.getInputStream(zipEntry), new File(outputDirectory, zipEntry.getName()),
                                zipEntry.getTime());
                    }
                }
            }
        }
    }

    private static void copyStreamToFile(InputStream in, File outputFile, long timestamp) throws IOException {
        if (in == null) {
            return;
        }
        outputFile.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(outputFile);
        try {
            IOUtil.copy(in, out);
        } finally {
            in.close();
            out.close();
        }
        if (timestamp > 0) {
            outputFile.setLastModified(timestamp);
        }
    }
}
