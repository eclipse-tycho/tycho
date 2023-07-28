/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TychoVersion {

    private static final String TYCHO_VERSION = readVersion("version");

    private static final String TYCHO_SCM = readVersion("scm");

    private static final String TYCHO_BUILDNUMBER = readVersion("buildNumber");

    private static final String BND_VERSION = readVersion("bnd");

    private static Properties PROPERTIES;

    public static String getTychoVersion() {
        return TYCHO_VERSION;
    }

    public static String getBndVersion() {
        return BND_VERSION;
    }

    public static String getSCMInfo() {
        if ("${env.GIT_COMMIT}".equals(TYCHO_SCM)) {
            if ("${buildNumber}".equals(TYCHO_BUILDNUMBER)) {
                //no way then...
                return "local build";
            }
            return TYCHO_BUILDNUMBER;
        }
        return TYCHO_SCM;
    }

    private static synchronized String readVersion(String version) {
        if (PROPERTIES == null) {
            try (InputStream stream = TychoVersion.class.getResourceAsStream("version.properties")) {
                PROPERTIES = new Properties();
                PROPERTIES.load(stream);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return PROPERTIES.getProperty(version);
    }

}
