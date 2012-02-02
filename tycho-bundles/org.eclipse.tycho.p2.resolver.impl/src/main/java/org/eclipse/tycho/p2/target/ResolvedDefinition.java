/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

class ResolvedDefinition implements TargetPlatformContent {

    private Collection<? extends IInstallableUnit> units;
    private Collection<URI> artifactRepositories;

    public ResolvedDefinition(Collection<? extends IInstallableUnit> units, Collection<URI> artifactRepositories) {
        this.units = units;
        this.artifactRepositories = artifactRepositories;
    }

    public Collection<? extends IInstallableUnit> getUnits() {
        return units;
    }

    public Collection<URI> getArtifactRepositoryLocations() {
        return artifactRepositories;
    }
}
