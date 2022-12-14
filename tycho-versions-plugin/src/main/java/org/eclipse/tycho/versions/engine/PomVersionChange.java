/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import org.eclipse.tycho.versions.pom.PomFile;

public class PomVersionChange extends VersionChange {
    private final PomFile pom;

    public PomVersionChange(PomFile pom, String newVersion) {
        this(pom, pom.getVersion(), newVersion);
    }

    public PomVersionChange(PomFile pom, String version, String newVersion) {
        super(version, newVersion);
        this.pom = pom;
    }

    public String getGroupId() {
        return pom.getGroupId();
    }

    public String getArtifactId() {
        return pom.getArtifactId();
    }

    public PomFile getProject() {
        return pom;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 17 * hash + pom.getGroupId().hashCode();
        hash = 17 * hash + pom.getArtifactId().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || //
                (obj instanceof PomVersionChange other && //
                        super.equals(other) && //
                        pom.getGroupId().equals(other.pom.getGroupId()) && //
                        pom.getArtifactId().equals(other.getArtifactId()));
    }

    @Override
    public String toString() {
        return pom.getGroupId() + ":" + pom.getArtifactId() + ":" + super.toString();
    }
}
