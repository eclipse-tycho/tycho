/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.eclipse.tycho.versions.pom.PomFile;

public class VersionChange {
    protected final String newVersion;

    private final PomFile pom;

    private final String version;

    public VersionChange(PomFile pom, String newVersion) {
        this(pom, pom.getVersion(), newVersion);
    }

    public VersionChange(PomFile pom, String version, String newVersion) {
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

    public PomFile getProject() {
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

    @Override
    public String toString() {
        return pom.getGroupId() + ":" + pom.getArtifactId() + ":" + version + " => " + newVersion;
    }
}
