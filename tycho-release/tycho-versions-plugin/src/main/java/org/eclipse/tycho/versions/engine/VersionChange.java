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
package org.eclipse.tycho.versions.engine;

import org.eclipse.tycho.versions.pom.MutablePomFile;

public class VersionChange {
    protected final String newVersion;

    private final MutablePomFile pom;

    private final String version;

    public VersionChange(MutablePomFile pom, String newVersion) {
        this(pom, pom.getVersion(), newVersion);
    }

    public VersionChange(MutablePomFile pom, String version, String newVersion) {
        this.pom = pom;
        this.version = Versions.toCanonicalVersion(version);
        this.newVersion = Versions.toCanonicalVersion(newVersion);
    }

    public String getGroupId() {
        return pom.getGroupId();
    }

    public String getArtifactId() {
        return pom.getArtifactId();
    }

    public String getVersion() {
        return version;
    }

    public MutablePomFile getProject() {
        return pom;
    }

    public String getNewVersion() {
        return newVersion;
    }

    @Override
    public int hashCode() {
        int hash = version.hashCode();
        hash = 17 * hash + newVersion.hashCode();
        hash = 17 * hash + pom.getGroupId().hashCode();
        hash = 17 * hash + pom.getArtifactId().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof VersionChange)) {
            return false;
        }

        VersionChange other = (VersionChange) obj;

        return version.equals(other.version) && newVersion.equals(other.newVersion)
                && pom.getGroupId().equals(other.pom.getGroupId()) && pom.getArtifactId().equals(other.getArtifactId());
    }
}
