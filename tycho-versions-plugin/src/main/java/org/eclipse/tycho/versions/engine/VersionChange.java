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

import java.util.Objects;

public class VersionChange {

    private final String version;
    private final String newVersion;

    public VersionChange(String version, String newVersion) {
        this.version = Versions.toCanonicalVersion(version);
        this.newVersion = Versions.toCanonicalVersion(newVersion);
    }

    public String getVersion() {
        return version;
    }

    public String getNewVersion() {
        return newVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, newVersion);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || //
                (obj instanceof VersionChange other && //
                        version.equals(other.version) && //
                        newVersion.equals(other.newVersion));
    }

    @Override
    public String toString() {
        return version + " => " + newVersion;
    }
}
