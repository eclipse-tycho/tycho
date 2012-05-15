/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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

    /**
     * @param file
     *            source jar file
     * @param packFile
     *            target pack file
     * @return <code>true</code> if the target pack file was created, <code>false</code> if the
     *         target file was not created
     */
    public boolean normalize(List<Artifact> pluginArtifacts, File file, File packFile) throws IOException {
        // read eclipse.inf from the input jar file
        // create a temp jar file with updated eclipse.inf
        // pack the temp jar
        // delete the temp jar

        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            if (eclipseInf.shouldPack() && !eclipseInf.isPackNormalized() && !isSigned(jarFile)) {
                log.info("Pack200 nomalizing jar " + file.getAbsolutePath());

                eclipseInf.setPackNormalized();

                File tmpFile = File.createTempFile(file.getName(), ".prepack");
                try {
                    updateEclipseInf(jarFile, eclipseInf, tmpFile);
                    getPack200().pack(pluginArtifacts, tmpFile, packFile);
                } finally {
                    if (!tmpFile.delete()) {
                        throw new IOException("Could not delete temporary file " + tmpFile.getAbsolutePath());
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

    private Pack200Wrapper getPack200() {
        // pack200 in java 6 and earlier has a memory leak that results in eventual OOME for large multimodule projects
        // the OOME is triggered by repetitive in-process execution of pack200 for different input jars
        // to workaround, fork pack200 to a separate JVM for each jar
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6888127

        try {
            // java.nio.file.FileSystem was introduced in java 7, so this will fail with CNFE on earlier jdk versions
            // this is how ant detects java7 too, see org.apache.tools.ant.util.JavaEnvUtils
            Class.forName("java.nio.file.FileSystem");
            return new Pack200Wrapper();
        } catch (ClassNotFoundException mustBeJava6orEarlier) {
            return new ForkedPack200Wrapper();
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
        getPack200().unpack(pluginArtifacts, packFile, jarFile);
    }

    public boolean pack(List<Artifact> pluginArtifacts, File file, File packFile) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            if (eclipseInf.shouldPack() && eclipseInf.isPackNormalized()) {
                log.info("Pack200 packing jar " + file.getAbsolutePath());
                getPack200().pack(pluginArtifacts, file, packFile);
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
