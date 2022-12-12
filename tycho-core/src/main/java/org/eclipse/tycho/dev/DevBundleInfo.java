/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.dev;

import java.io.File;

import org.eclipse.tycho.ArtifactKey;

public class DevBundleInfo {
    private final ArtifactKey artifactKey;
    private final File location;
    private final String devEntries;

    public DevBundleInfo(ArtifactKey artifactKey, File location, String devEntries) {
        this.artifactKey = artifactKey;
        this.location = location;
        this.devEntries = devEntries;
    }

    public String getSymbolicName() {
        return artifactKey.getId();
    }

    public File getLocation() {
        return location;
    }

    public String getDevEntries() {
        return devEntries;
    }

    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }
}
