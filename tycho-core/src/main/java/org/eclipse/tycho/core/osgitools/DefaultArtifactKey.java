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
package org.eclipse.tycho.core.osgitools;

import org.eclipse.tycho.ArtifactKey;

public class DefaultArtifactKey implements org.eclipse.tycho.ArtifactKey {
    private final String type;

    private final String id;

    private final String version;

    public DefaultArtifactKey(String type, String id, String version) {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    public static DefaultArtifactKey fromManifest(OsgiManifest manifest) {
        return new DefaultArtifactKey(TYPE_ECLIPSE_PLUGIN, manifest.getBundleSymbolicName(),
                manifest.getBundleVersion());
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
    public String getType() {
        return type;
    }

    /**
     * Eclipse/OSGi artifact id. Can differ from Maven artifactId.
     */
    public String getId() {
        return id;
    }

    /**
     * Eclipse/OSGi artifact version. Can differ from Maven version. For maven projects, this
     * version corresponds to version specified in the project sources and does not reflect
     * qualifier expansion.
     */
    public String getVersion() {
        return version;
    }
}
