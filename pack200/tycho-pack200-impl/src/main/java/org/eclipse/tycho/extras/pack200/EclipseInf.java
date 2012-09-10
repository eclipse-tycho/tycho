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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.codehaus.plexus.util.IOUtil;

/**
 * http://wiki.eclipse.org/JarProcessor_Options
 */
public class EclipseInf {

    public static final String PATH_ECLIPSEINF = "META-INF/eclipse.inf";

    public static final String TRUE = "true";

    public static final String PACK200_CONDITIONED = "pack200.conditioned";

    private final Properties properties;

    private EclipseInf(Properties properties) {
        this.properties = properties;
    }

    public boolean shouldPack() {
        return !Boolean.parseBoolean(properties.getProperty("jarprocessor.exclude"))
                && !Boolean.parseBoolean(properties.getProperty("jarprocessor.exclude.pack"));
    }

    public boolean shouldSign() {
        return !Boolean.parseBoolean(properties.getProperty("jarprocessor.exclude"))
                && !Boolean.parseBoolean(properties.getProperty("jarprocessor.exclude.sign"));
    }

    public boolean isPackNormalized() {
        return Boolean.parseBoolean(properties.getProperty(PACK200_CONDITIONED));
    }

    public void setPackNormalized() {
        properties.put(PACK200_CONDITIONED, TRUE);
    }

    public static EclipseInf readEclipseInf(JarFile jarFile) throws IOException {
        Properties properties = new Properties();

        ZipEntry entry = jarFile.getEntry(PATH_ECLIPSEINF);
        if (entry != null) {
            InputStream is = jarFile.getInputStream(entry);
            try {
                properties.load(is);
            } finally {
                IOUtil.close(is);
            }
            return new EclipseInf(properties);
        }

        return null;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        properties.store(buf, null);
        return buf.toByteArray();
    }
}
