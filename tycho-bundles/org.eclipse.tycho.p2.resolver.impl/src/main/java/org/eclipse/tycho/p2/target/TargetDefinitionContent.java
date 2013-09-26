/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
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

/**
 * Result from resolving a target definition: A list of installable units, plus the URLs of the
 * artifact repositories containing the corresponding artifacts.
 */
public class TargetDefinitionContent {

    private Collection<IInstallableUnit> units;
    private Collection<URI> artifactRepositories;

    public TargetDefinitionContent(Collection<IInstallableUnit> units, Collection<URI> artifactRepositories) {
        this.units = units;
        this.artifactRepositories = artifactRepositories;
    }

    public Collection<IInstallableUnit> getUnits() {
        return units;
    }

    public Collection<URI> getArtifactRepositoryLocations() {
        return artifactRepositories;
    }
}
