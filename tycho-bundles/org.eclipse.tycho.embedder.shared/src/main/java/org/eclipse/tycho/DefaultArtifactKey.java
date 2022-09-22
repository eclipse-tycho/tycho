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

import java.util.Objects;

public class DefaultArtifactKey implements org.eclipse.tycho.ArtifactKey {
    private final String type;

    private final String id;

    private final String version;

    public DefaultArtifactKey(String type, String id) {
        this(type, id, "0.0.0");
    }

    public DefaultArtifactKey(String type, String id, String version) {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getId(), getVersion());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof ArtifactKey other && //
                        Objects.equals(getType(), other.getType()) && //
                        Objects.equals(getId(), other.getId()) && //
                        Objects.equals(getVersion(), other.getVersion()));
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
