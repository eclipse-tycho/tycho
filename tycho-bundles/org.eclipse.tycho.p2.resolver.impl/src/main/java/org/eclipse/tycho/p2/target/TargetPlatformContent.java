/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.Collection;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class TargetPlatformContent {

    private Collection<? extends IInstallableUnit> units;
    private Collection<URI> artifactRepositories;

    public TargetPlatformContent(Collection<? extends IInstallableUnit> units, Collection<URI> artifactRepositories) {
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
