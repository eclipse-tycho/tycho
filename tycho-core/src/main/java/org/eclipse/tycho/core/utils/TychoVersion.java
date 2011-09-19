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
package org.eclipse.tycho.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TychoVersion {

    private static final String DEFAULT_TYCHO_VERSION = "0.13.0-SNAPSHOT";

    public static String getTychoVersion() {
        ClassLoader cl = TychoVersion.class.getClassLoader();
        InputStream is = cl.getResourceAsStream("META-INF/maven/org.eclipse.tycho/tycho-core/pom.properties");
        String version = DEFAULT_TYCHO_VERSION;
        if (is != null) {
            try {
                try {
                    Properties p = new Properties();
                    p.load(is);
                    version = p.getProperty("version", DEFAULT_TYCHO_VERSION);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                // getLogger().debug("Could not read pom.properties", e);
            }
        }
        return version;
    }

}
