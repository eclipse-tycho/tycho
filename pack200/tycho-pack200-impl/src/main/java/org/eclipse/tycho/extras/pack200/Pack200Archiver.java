/*******************************************************************************
 * Copyright (c) 2012, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.codehaus.plexus.util.IOUtil;

@Component(role = Pack200Archiver.class)
public class Pack200Archiver {

    @Requirement
    private Logger log;

    private Pack200Wrapper packWrapper = new Pack200Wrapper();

    /**
     * @param file
     *            source jar file
     * @param packFile
     *            target pack file
     * @return <code>true</code> if the target pack file was created, <code>false</code> if the
     *         target file was not created
     */
    public boolean normalize(List<Artifact> pluginArtifacts, File file, File packFile) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {

            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            assertSupportedEclipseInf(eclipseInf);
            if (eclipseInf == null || (eclipseInf.shouldPack() && !eclipseInf.isPackNormalized())) {
                if (isSigned(jarFile)) {
                    throw new IOException("pack200:normalize cannot be called for signed jar " + file);
                }
                log.info("Pack200 normalizing jar " + file.getAbsolutePath());

                File tmpFile = null;
                if (eclipseInf != null) {
                    eclipseInf.setPackNormalized();
                    tmpFile = File.createTempFile(file.getName(), ".prepack");
                    updateEclipseInf(jarFile, eclipseInf, tmpFile);
                }

                try {
                    packWrapper.pack(pluginArtifacts, tmpFile != null ? tmpFile : file, packFile);
                } finally {
                    if (tmpFile != null) {
                        if (!tmpFile.delete()) {
                            throw new IOException("Could not delete temporary file " + tmpFile.getAbsolutePath());
                        }
                    }
                }
                return true;
            }
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    protected void assertSupportedEclipseInf(EclipseInf eclipseInf) throws IOException {
        if (eclipseInf != null && eclipseInf.shouldPack() && eclipseInf.shouldSign()) {
            throw new IOException("Pack200 and jar signing cannot be both enabled in " + EclipseInf.PATH_ECLIPSEINF
                    + ". See bug 388629.");
        }
    }

    private void updateEclipseInf(JarFile jarFile, EclipseInf eclipseInf, File tmpFile) throws IOException {
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
        try {
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
        } finally {
            IOUtil.close(jos);
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

    public void unpack(List<Artifact> pluginArtifacts, File packFile, File jarFile) throws IOException {
        packWrapper.unpack(pluginArtifacts, packFile, jarFile);
    }

    public boolean pack(List<Artifact> pluginArtifacts, File file, File packFile) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            assertSupportedEclipseInf(eclipseInf);
            if (eclipseInf == null || (eclipseInf.shouldPack() && eclipseInf.isPackNormalized())) {
                log.info("Pack200 packing jar " + file.getAbsolutePath());
                packWrapper.pack(pluginArtifacts, file, packFile);
                return true;
            }
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

}
