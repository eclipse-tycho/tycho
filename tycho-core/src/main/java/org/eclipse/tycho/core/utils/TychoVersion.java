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

import org.codehaus.plexus.util.IOUtil;

public class TychoVersion {

    static {
        readVersions();
    }

    private static String TYCHO_VERSION;
    private static String JDT_CORE_VERSION;
    private static String JDT_APT_VERSION;

    public static String getTychoVersion() {
        return TYCHO_VERSION;
    }

    public static String getJdtCoreVersion() {
        return JDT_CORE_VERSION;
    }

    public static String getJdtAptVersion() {
        return JDT_APT_VERSION;
    }

    private static void readVersions() {
        InputStream stream = TychoVersion.class.getResourceAsStream("version.properties");
        try {
            Properties p = new Properties();
            p.load(stream);
            TYCHO_VERSION = p.getProperty("version");
            JDT_CORE_VERSION = p.getProperty("jdtCoreVersion");
            JDT_APT_VERSION = p.getProperty("jdtAptVersion");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtil.close(stream);
        }
    }

}
