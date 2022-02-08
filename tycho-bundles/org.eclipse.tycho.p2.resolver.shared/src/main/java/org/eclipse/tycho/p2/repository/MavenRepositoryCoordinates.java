/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

import java.util.Objects;

import org.eclipse.tycho.core.shared.MavenContext;

/**
 * Coordinates (groupId, artifactId, version, classifier, extension) of an artifact in the local
 * Maven repository.
 */
public final class MavenRepositoryCoordinates {

    private static final String DEFAULT_TYPE = "jar";

    private final GAV gav;
    private final String classifier;
    private final String type;

    public MavenRepositoryCoordinates(GAV gav, String classifier, String type) {
        this.gav = gav; // TODO check for null?
        this.classifier = classifier;
        this.type = DEFAULT_TYPE.equals(type) ? null : type;
    }

    public MavenRepositoryCoordinates(String groupId, String artifactId, String version, String classifier,
            String type) {
        this(new GAV(groupId, artifactId, version), classifier, type);
    }

    public GAV getGav() {
        return gav;
    }

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    /**
     * Returns the (symbolic) artifact version, i.e. without any "SNAPSHOT" expansion.
     */
    public String getVersion() {
        return gav.getVersion();
    }

    public String getClassifier() {
        return classifier;
    }

    /**
     * The artifact type that is used to determine its extension, or <code>null</code> if the type
     * is unknown.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the local Maven repository path corresponding to the these coordinates.
     */
    public String getLocalRepositoryPath(MavenContext mavenContext) {
        return RepositoryLayoutHelper.getRelativePath(getGav(), getClassifier(), getType(), mavenContext);
    }

    @Override
    public String toString() {
        // same format as e.g. used by the maven-dependency-plugin
        StringBuilder result = new StringBuilder();
        result.append(getGroupId());
        result.append(':');
        result.append(getArtifactId());
        result.append(':');
        result.append(Objects.requireNonNullElse(getType(), DEFAULT_TYPE));
        if (getClassifier() != null) {
            result.append(':');
            result.append(getClassifier());
        }
        result.append(':');
        result.append(getVersion());
        return result.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gav == null) ? 0 : gav.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MavenRepositoryCoordinates))
            return false;

        MavenRepositoryCoordinates other = (MavenRepositoryCoordinates) obj;
        return eq(gav, other.gav) && eq(classifier, other.classifier) && eq(type, other.type);
    }

    private static <T> boolean eq(T left, T right) {
        if (left == right)
            return true;
        else if (left == null)
            return false;
        else
            return left.equals(right);
    }
}
