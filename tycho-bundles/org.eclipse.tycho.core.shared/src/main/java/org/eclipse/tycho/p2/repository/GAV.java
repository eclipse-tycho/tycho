/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.repository;

import java.util.Objects;

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
        return o instanceof GAV other && //
                Objects.equals(groupId, other.getGroupId()) && //
                Objects.equals(artifactId, other.getArtifactId()) && //
                Objects.equals(version, other.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
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
    public static GAV parse(String line) throws IllegalArgumentException {
        int currentIndex = 0;
        int colonIndex = -1;

        colonIndex = nextColonIndex(line, currentIndex);
        String groupId = substring(line, currentIndex, colonIndex);

        currentIndex = colonIndex + 1;
        colonIndex = nextColonIndex(line, currentIndex);
        String artifactId = substring(line, currentIndex, colonIndex);

        currentIndex = colonIndex + 1;
        String version = substring(line, currentIndex, line.length());

        return new GAV(groupId, artifactId, version);
    }

    private static String substring(String str, int start, int end) {
        String substring = str.substring(start, end);
        return "".equals(substring) ? null : substring;
    }

    private static int nextColonIndex(String line, int pos) throws IllegalArgumentException {
        int colonIndex = line.indexOf(':', pos);
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid line: '" + line + "'");
        }
        return colonIndex;
    }

    @Override
    public String toString() {
        return toExternalForm();
    }
}
