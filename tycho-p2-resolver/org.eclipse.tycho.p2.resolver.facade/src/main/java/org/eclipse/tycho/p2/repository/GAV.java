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
package org.eclipse.tycho.p2.repository;

public class GAV {
    private String groupId;

    private String artifactId;

    private String version;

    public GAV(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GAV)) {
            return false;
        }
        GAV other = (GAV) o;

        return equals(groupId, other.getGroupId()) && equals(artifactId, other.getArtifactId())
                && equals(version, other.getVersion());
    }

    private static boolean equals(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        }
        return str1.equals(str2);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + (groupId != null ? groupId.hashCode() : 0);
        hash = hash * 31 + (artifactId != null ? artifactId.hashCode() : 0);
        hash = hash * 31 + (version != null ? version.hashCode() : 0);
        return hash;
    }

    public String toExternalForm() {
        StringBuilder sb = new StringBuilder();

        sb.append(groupId).append(':').append(artifactId).append(':').append(version);

        return sb.toString();
    }

    public static GAV parse(String str) {
        if (str == null || str.trim().length() <= 0) {
            return null;
        }

        int p, c;

        p = 0;
        c = nextColonIndex(str, p);
        String groupId = substring(str, p, c);

        p = c + 1;
        c = nextColonIndex(str, p);
        String artifactId = substring(str, p, c);

        p = c + 1;
        c = str.length();
        String version = substring(str, p, c);

        return new GAV(groupId, artifactId, version);
    }

    private static String substring(String str, int start, int end) {
        String substring = str.substring(start, end);
        return "".equals(substring) ? null : substring;
    }

    private static int nextColonIndex(String str, int pos) {
        int idx = str.indexOf(':', pos);
        if (idx < 0)
            throw new IllegalArgumentException("Invalid portable string: " + str);
        return idx;
    }

    @Override
    public String toString() {
        return toExternalForm();
    }
}
