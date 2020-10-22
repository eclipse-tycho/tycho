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
package org.eclipse.tycho;

public class DefaultArtifactKey implements org.eclipse.tycho.ArtifactKey {
    private final String type;

    private final String id;

    private final String version;

    public DefaultArtifactKey(String type, String id, String version) {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    @Override
    public int hashCode() {
        int hash = getType().hashCode();
        hash = 17 * hash + getId().hashCode();
        hash = 17 * hash + getVersion().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ArtifactKey)) {
            return false;
        }

        ArtifactKey other = (ArtifactKey) obj;

        return getType().equals(other.getType()) && getId().equals(other.getId())
                && getVersion().equals(other.getVersion());
    }

    @Override
    public String toString() {
        return getType() + ":" + getId() + ":" + getVersion();
    }

    /**
     * @see ProjectType
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Eclipse/OSGi artifact id. Can differ from Maven artifactId.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Eclipse/OSGi artifact version. Can differ from Maven version. For maven projects, this
     * version corresponds to version specified in the project sources and does not reflect
     * qualifier expansion.
     */
    @Override
    public String getVersion() {
        return version;
    }
}
