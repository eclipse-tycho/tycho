/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

public class VersioningHelper {

    public static final String QUALIFIER = "qualifier";

    public static String expandQualifier(String version, String buildQualifier) {
        String unqualifiedVersion = stripQualifier(version);
        if (unqualifiedVersion == null) {
            // nothing to expand
            return version;
        } else {
            return appendSegment(unqualifiedVersion, buildQualifier);
        }
    }

    /**
     * @return the version without '.qualifier' suffix, or <code>null</code> if the version didn't
     *         have that suffix.
     */
    private static String stripQualifier(String version) {
        if (version.endsWith("." + QUALIFIER)) {
            int qualifierIndex = version.length() - VersioningHelper.QUALIFIER.length();
            return version.substring(0, qualifierIndex - 1);
        }
        return null;
    }

    private static String appendSegment(String version, String newSegment) {
        if (newSegment == null || "".equals(newSegment)) {
            return version;
        } else {
            return version + "." + newSegment;
        }
    }

}
