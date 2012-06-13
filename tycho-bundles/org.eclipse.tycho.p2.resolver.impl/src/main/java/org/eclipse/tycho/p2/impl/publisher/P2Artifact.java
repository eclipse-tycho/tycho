/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

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
    private final Set<Object> installableUnits;
    private final Object artifactDescriptor;

    public P2Artifact(File location, Collection<IInstallableUnit> installableUnits,
            IArtifactDescriptor artifactDescriptor) {
        this.location = location;
        this.installableUnits = Collections.unmodifiableSet(toRawSet(installableUnits));
        this.artifactDescriptor = artifactDescriptor;
    }

    public File getLocation() {
        return location;
    }

    public Set<Object> getInstallableUnits() {
        return installableUnits;
    }

    public Object getArtifactDescriptor() {
        return artifactDescriptor;
    }

    private static <T> Set<Object> toRawSet(Collection<T> set) {
        return new LinkedHashSet<Object>(set);
    }
}
