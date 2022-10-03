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
package org.eclipse.tycho.p2.publisher;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.metadata.IP2Artifact;

public class P2Artifact implements IP2Artifact {

    private final File location;
    private final Set<IInstallableUnit> installableUnits;
    private final IArtifactDescriptor artifactDescriptor;

    public P2Artifact(File location, Collection<IInstallableUnit> installableUnits,
            IArtifactDescriptor artifactDescriptor) {
        this.location = location;
        this.installableUnits = Collections.unmodifiableSet(new LinkedHashSet<>(installableUnits));
        this.artifactDescriptor = artifactDescriptor;
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    @Override
    public IArtifactDescriptor getArtifactDescriptor() {
        return artifactDescriptor;
    }

}
