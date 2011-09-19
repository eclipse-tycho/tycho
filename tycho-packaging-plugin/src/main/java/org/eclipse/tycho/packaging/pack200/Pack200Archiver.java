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
package org.eclipse.tycho.packaging.pack200;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

public class Pack200Archiver {

    private Map<? extends String, ? extends String> properties;

    private File sourceJar;

    private File destFile;

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public Map<? extends String, ? extends String> getProperties() {
        if (properties == null) {
            return new LinkedHashMap<String, String>();
        }
        return properties;
    }

    public void setProperties(Map<? extends String, ? extends String> properties) {
        this.properties = properties;
    }

    public void createArchive() throws IOException {
        // Create the Packer object
        Packer packer = Pack200.newPacker();

        packer.properties().putAll(getProperties());
        JarFile jarFile = new JarFile(getSourceJar());
        FileOutputStream fos = new FileOutputStream(getDestFile());
        // Call the packer
        packer.pack(jarFile, fos);
        jarFile.close();
        fos.close();
    }

    public File getSourceJar() {
        return this.sourceJar;
    }

    public void setSourceJar(File sourceJar) {
        this.sourceJar = sourceJar;
    }
}
