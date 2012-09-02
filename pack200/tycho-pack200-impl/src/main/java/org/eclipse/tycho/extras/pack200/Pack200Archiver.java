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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

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
        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf.assertNotPresent(jarFile);
            if (isSigned(jarFile)) {
                throw new IOException("pack200:normalize cannot be called for signed jar " + file);
            }
            log.info("Pack200 nomalizing jar " + file.getAbsolutePath());
            getPack200().pack(pluginArtifacts, file, packFile);
            return true;
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
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

    public void unpack(List<Artifact> pluginArtifacts, File packFile, File jarFile) throws IOException {
        getPack200().unpack(pluginArtifacts, packFile, jarFile);
    }

    public boolean pack(List<Artifact> pluginArtifacts, File file, File packFile) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf.assertNotPresent(jarFile);
            log.info("Pack200 packing jar " + file.getAbsolutePath());
            getPack200().pack(pluginArtifacts, file, packFile);
            return true;
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
