/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #663 - Access to the tycho .cache directory is not properly synchronized 
 *******************************************************************************/
package org.eclipse.sisu.osgi.launching.equinox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.OsgiManifest;
import org.eclipse.tycho.OsgiManifestParserException;
import org.eclipse.tycho.locking.facade.FileLockService;

public class ManifestReader {

    private final Map<String, OsgiManifest> manifestCache = new HashMap<>();

    @Requirement
    private FileLockService fileLockService;

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

}
