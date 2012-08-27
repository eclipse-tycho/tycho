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

    private final boolean updateExportedPackages;

    public VersionChange(MutablePomFile pom, String newVersion) {
        this(pom, pom.getEffectiveVersion(), newVersion);
    }

    public VersionChange(MutablePomFile pom, String newVersion, boolean updateExportedPackages) {
        this(pom, pom.getEffectiveVersion(), newVersion, updateExportedPackages);
    }

    public VersionChange(MutablePomFile pom, String version, String newVersion) {
        this(pom, version, newVersion, false);
    }

    public VersionChange(MutablePomFile pom, String version, String newVersion, boolean updateExportedPackages) {
        this.pom = pom;
        this.version = Versions.toCanonicalVersion(version);
        this.newVersion = Versions.toCanonicalVersion(newVersion);
        this.updateExportedPackages = updateExportedPackages;
    }

    public String getGroupId() {
        return pom.getEffectiveGroupId();
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

    public boolean isUpdateExportedPackages() {
        return updateExportedPackages;
    }

    @Override
    public int hashCode() {
        int hash = version.hashCode();
        hash = 17 * hash + newVersion.hashCode();
        hash = 17 * hash + pom.getEffectiveGroupId().hashCode();
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
                && pom.getEffectiveGroupId().equals(other.pom.getEffectiveGroupId())
                && pom.getArtifactId().equals(other.getArtifactId());
    }
}
