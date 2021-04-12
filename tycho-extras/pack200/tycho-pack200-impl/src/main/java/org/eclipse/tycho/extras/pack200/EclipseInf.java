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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * https://wiki.eclipse.org/JarProcessor_Options
 */
@Deprecated(forRemoval = true, since = "2.3.0")
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
            try (InputStream is = jarFile.getInputStream(entry)) {
                properties.load(is);
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
