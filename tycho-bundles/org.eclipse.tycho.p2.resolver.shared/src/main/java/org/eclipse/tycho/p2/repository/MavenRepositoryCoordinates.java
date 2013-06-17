/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.repository;

/**
 * Coordinates (groupId, artifactId, version, classifier, extension) of an artifact in the local
 * Maven repository.
 */
public final class MavenRepositoryCoordinates {

    public static final String DEFAULT_EXTENSION = RepositoryLayoutHelper.DEFAULT_EXTERNSION;

    private final GAV gav;
    private final String classifier;
    private final String extension;

    public MavenRepositoryCoordinates(GAV gav, String classifier, String extension) {
        this.gav = gav; // TODO check for null?
        this.classifier = classifier;
        this.extension = DEFAULT_EXTENSION.equals(extension) ? null : extension;
    }

    public MavenRepositoryCoordinates(String groupId, String artifactId, String version, String classifier,
            String extension) {
        this(new GAV(groupId, artifactId, version), classifier, extension);
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
     * The artifact file extension, or <code>null</code> for the default extension <code>jar</code>.
     */
    public String getExtension() {
        return extension;
    }

    public String getExtensionOrDefault() {
        if (extension == null)
            return DEFAULT_EXTENSION;
        else
            return extension;
    }

    /**
     * Returns the local Maven repository path corresponding to the these coordinates.
     */
    public String getLocalRepositoryPath() {
        return RepositoryLayoutHelper.getRelativePath(getGav(), getClassifier(), getExtension());
    }

    @Override
    public String toString() {
        // same format as e.g. used by the maven-dependency-plugin
        StringBuilder result = new StringBuilder();
        result.append(getGroupId());
        result.append(':');
        result.append(getArtifactId());
        result.append(':');
        result.append(getExtensionOrDefault());
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
        result = prime * result + ((extension == null) ? 0 : extension.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MavenRepositoryCoordinates))
            return false;

        MavenRepositoryCoordinates other = (MavenRepositoryCoordinates) obj;
        return eq(gav, other.gav) && eq(classifier, other.classifier) && eq(extension, other.extension);
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
