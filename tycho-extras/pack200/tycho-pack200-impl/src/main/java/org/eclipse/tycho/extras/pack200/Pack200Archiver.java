/*******************************************************************************
 * Copyright (c) 2012, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pack200;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component(role = Pack200Archiver.class)
public class Pack200Archiver {

    @Requirement
    private Logger log;

    private Pack200Wrapper packWrapper = new Pack200Wrapper();
    private ForkedPack200Wrapper forkedPackWrapper = new ForkedPack200Wrapper();

    /**
     * @param file
     *            source jar file
     * @param packFile
     *            target pack file
     * @return <code>true</code> if the target pack file was created, <code>false</code> if the
     *         target file was not created
     */
    public boolean normalize(List<Artifact> pluginArtifacts, File file, File packFile, boolean fork) throws IOException {
        File jarToBePacked = file;
        boolean shouldNormalize = false;
        boolean jarToBePackedIsTempFile = false;
        try (JarFile jarFile = new JarFile(file)) {
            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            assertSupportedEclipseInf(eclipseInf);
            if (eclipseInf == null || (eclipseInf.shouldPack() && !eclipseInf.isPackNormalized())) {
                if (isSigned(jarFile)) {
                    throw new IOException("pack200:normalize cannot be called for signed jar " + file);
                }
                shouldNormalize = true;
                if (eclipseInf != null) {
                    eclipseInf.setPackNormalized();
                    jarToBePacked = File.createTempFile(file.getName(), ".prepack");
                    jarToBePackedIsTempFile = true;
                    updateEclipseInf(jarFile, eclipseInf, jarToBePacked);
                }

            }
        }
        if (shouldNormalize) {
            try {
                log.info("Pack200 normalizing jar " + file.getAbsolutePath());
                getPackWrapper(fork).pack(pluginArtifacts, jarToBePacked, packFile);
            } finally {
                if (jarToBePackedIsTempFile) {
                    if (!jarToBePacked.delete()) {
                        throw new IOException("Could not delete temporary file " + jarToBePacked.getAbsolutePath());
                    }
                }
            }
            return true;
        }
        return false;
    }

    private Pack200Wrapper getPackWrapper(boolean fork) {
        return fork ? forkedPackWrapper : packWrapper;
    }

    protected void assertSupportedEclipseInf(EclipseInf eclipseInf) throws IOException {
        if (eclipseInf != null && eclipseInf.shouldPack() && eclipseInf.shouldSign()) {
            throw new IOException("Pack200 and jar signing cannot be both enabled in " + EclipseInf.PATH_ECLIPSEINF
                    + ". See bug 388629.");
        }
    }

    private void updateEclipseInf(JarFile jarFile, EclipseInf eclipseInf, File tmpFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().equals(EclipseInf.PATH_ECLIPSEINF)) {
                    copyJarEntry(jarFile, entry, jos);
                }
            }
            JarEntry entry = new JarEntry(EclipseInf.PATH_ECLIPSEINF);
            jos.putNextEntry(entry);
            jos.write(eclipseInf.toByteArray());
            jos.closeEntry();
        }
    }

    private boolean isSigned(JarFile jarFile) throws IOException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("META-INF/") && name.endsWith(".SF")) {
                return true;
            }
        }
        return false;
    }

    private void copyJarEntry(JarFile jarFile, JarEntry entry, JarOutputStream jos) throws IOException {
        jos.putNextEntry(entry);

        InputStream is = jarFile.getInputStream(entry);
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            jos.write(buf, 0, n);
        }

        jos.closeEntry();
    }

    public void unpack(List<Artifact> pluginArtifacts, File packFile, File jarFile, boolean fork) throws IOException {
        getPackWrapper(fork).unpack(pluginArtifacts, packFile, jarFile);
    }

    public boolean pack(List<Artifact> pluginArtifacts, File file, File packFile, boolean fork) throws IOException {
        EclipseInf eclipseInf = null;
        try (JarFile jarFile = new JarFile(file)) {
            eclipseInf = EclipseInf.readEclipseInf(jarFile);
        }
        assertSupportedEclipseInf(eclipseInf);
        if (eclipseInf == null || (eclipseInf.shouldPack() && eclipseInf.isPackNormalized())) {
            log.info("Pack200 packing jar " + file.getAbsolutePath());
            getPackWrapper(fork).pack(pluginArtifacts, file, packFile);
            return true;
        }
        return false;
    }

}
