/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.osgi.framework.Version;

public class Versions {
    public static final String SUFFIX_QUALIFIER = ".qualifier";

    public static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    public static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }

        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length()) + SUFFIX_QUALIFIER;
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
        return toCanonicalVersion(a).equals(toCanonicalVersion(b));
    }

    public static void assertIsVersionDiff(String versionDiff) throws NumberFormatException, IllegalArgumentException {
        String[] segments = versionDiff.split("\\.");
        if (segments.length != 3) {
            throw new IllegalArgumentException("A versionDiff must be made of 3 numeric segements");
        }
        for (int i = 0; i < segments.length; i++) {
            Integer.parseInt(segments[i]);
        }
    }

    public static boolean isNullVersionDiff(String versionDiff) {
        String[] segments = versionDiff.split("\\.");
        for (int i = 0; i < segments.length; i++) {
            if (Integer.parseInt(segments[i]) != 0) {
                return false;
            }
        }
        return true;
    }

}
