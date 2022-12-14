/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.versions.engine;

import java.io.File;

import org.osgi.framework.Version;

public class Versions {
    private static final String SUFFIX_QUALIFIER = ".qualifier";

    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    public static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length()) + SUFFIX_QUALIFIER;
        }

        return version;
    }

    /**
     * Returns the version without trailing ".qualifier" or "-SNAPSHOT".
     */
    public static String toBaseVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length());
        }
        if (version.endsWith(SUFFIX_QUALIFIER)) {
            return version.substring(0, version.length() - SUFFIX_QUALIFIER.length());
        }

        return version;
    }

    public static void assertIsOsgiVersion(String version) throws NumberFormatException, IllegalArgumentException,
            NullPointerException {
        new Version(version);
    }

    public static String toMavenVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_QUALIFIER)) {
            return version.substring(0, version.length() - SUFFIX_QUALIFIER.length()) + SUFFIX_SNAPSHOT;
        }

        return version;
    }

    public static boolean isVersionEquals(String a, String b) {
        return eq(toCanonicalVersion(a), toCanonicalVersion(b));
    }

    public static String validateOsgiVersion(String version, File location) {
        try {
            Versions.assertIsOsgiVersion(Versions.toCanonicalVersion(version));
        } catch (RuntimeException e) {
            return String.format("Version %s is not valid for %s", version, location);
        }
        return null;
    }

    public static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }
}
