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

    /**
     * Parse a line in the form "g:a:v"
     * 
     * @throws IllegalArgumentException
     *             if line is not well-formed
     */
    public static GAV parse(String line) {
        int currentIndex = 0;
        int colonIndex = -1;

        colonIndex = nextColonIndex(line, currentIndex);
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid line: '" + line + "'");
        }
        String groupId = substring(line, currentIndex, colonIndex);

        currentIndex = colonIndex + 1;
        colonIndex = nextColonIndex(line, currentIndex);
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid line: '" + line + "'");
        }
        String artifactId = substring(line, currentIndex, colonIndex);

        currentIndex = colonIndex + 1;
        String version = substring(line, currentIndex, line.length());

        return new GAV(groupId, artifactId, version);
    }

    private static String substring(String str, int start, int end) {
        String substring = str.substring(start, end);
        return "".equals(substring) ? null : substring;
    }

    private static int nextColonIndex(String str, int pos) {
        return str.indexOf(':', pos);
    }

    @Override
    public String toString() {
        return toExternalForm();
    }
}
