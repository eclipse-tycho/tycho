/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.artifact.Artifact;

@Deprecated(forRemoval = true, since = "2.3.0")
public class Pack200Wrapper {

    public static final String COMMAND_PACK = "pack";

    public static final String COMMAND_UNPACK = "unpack";

    public void pack(List<Artifact> pluginArtifacts, File jar, File pack) throws IOException {
        pack(jar, pack);
    }

    private void pack(File jar, File pack) throws IOException, FileNotFoundException {
        // 387541 apparently JarFile is required to preserve jar file signatures 

        try (JarFile is = new JarFile(jar)) {
            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(pack)))) {
                Packer packer = newPacker();
                packer.pack(is, os);
            }
        }
    }

    public void unpack(List<Artifact> pluginArtifacts, File packFile, File jarFile) throws IOException {
        unpack(packFile, jarFile);
    }

    protected void unpack(File packFile, File jarFile) throws IOException, FileNotFoundException {
        try (InputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(packFile)))) {
            try (JarOutputStream os = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)))) {
                Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(is, os);
            }
        }
    }

    private Packer newPacker() {
        Packer packer = Pack200.newPacker();
        // From Pack200.Packer javadoc:
        //    ... the segment limit may also need to be set to "-1", 
        //    to prevent accidental variation of segment boundaries as class file sizes change slightly
        packer.properties().put(Packer.SEGMENT_LIMIT, "-1");
        return packer;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Syntax: " + Pack200Wrapper.class.getSimpleName() + " pack|unpack fileFrom fileTo");
            System.exit(-1);
        }

        File fileFrom = new File(args[1]).getCanonicalFile();
        File fileTo = new File(args[2]).getCanonicalFile();

        if (COMMAND_PACK.equals(args[0])) {
            new Pack200Wrapper().pack(fileFrom, fileTo);
            System.exit(0);
        } else if (COMMAND_UNPACK.equals(args[0])) {
            new Pack200Wrapper().unpack(fileFrom, fileTo);
            System.exit(0);
        }

        System.err.println("Unknown/unsupported command " + args[0]);
        System.exit(-1);
    }

}
