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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPOutputStream;

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
    public boolean normalize(File file, File packFile) throws IOException {
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
                    JarInputStream is = new JarInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));
                    try {
                        OutputStream os = new BufferedOutputStream(new FileOutputStream(packFile));
                        try {
                            Packer packer = newPacker();
                            packer.pack(is, os);
                        } finally {
                            IOUtil.close(os);
                        }
                    } finally {
                        IOUtil.close(is);
                    }
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

    private Packer newPacker() {
        Packer packer = Pack200.newPacker();
        // From Pack200.Packer javadoc:
        //    ... the segment limit may also need to be set to "-1", 
        //    to prevent accidental variation of segment boundaries as class file sizes change slightly
        packer.properties().put(Packer.SEGMENT_LIMIT, "-1");
        return packer;
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

    public void unpack(File packFile, File jarFile) throws IOException {
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));
        try {
            Unpacker unpacker = Pack200.newUnpacker();
            unpacker.unpack(packFile, jos);
        } finally {
            IOUtil.close(jos);
        }
    }

    public boolean pack(File file, File packFile) throws IOException {
        JarFile jarFile = new JarFile(file);
        try {
            EclipseInf eclipseInf = EclipseInf.readEclipseInf(jarFile);
            if (eclipseInf.shouldPack() && eclipseInf.isPackNormalized()) {
                log.info("Pack200 packing jar " + file.getAbsolutePath());

                File jarpackgz = new File(file.getCanonicalPath() + ".pack.gz");
                OutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(jarpackgz)));
                try {
                    Packer packer = newPacker();
                    packer.pack(jarFile, os);
                } finally {
                    IOUtil.close(os);
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

}
