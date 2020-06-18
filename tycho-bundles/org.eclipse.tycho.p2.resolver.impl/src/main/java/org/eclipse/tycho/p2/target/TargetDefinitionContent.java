/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Bug 538144] Support other target locations (Directory, Features, Installations) 
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.repository.ArtifactRepositorySupplier;
import org.eclipse.tycho.repository.p2base.artifact.repository.RepositoryArtifactProvider;

/**
 * Result from resolving a target definition: A list of installable units, plus the URLs of the
 * artifact repositories containing the corresponding artifacts.
 */
public class TargetDefinitionContent {

    private Collection<IInstallableUnit> units;
    private ArtifactRepositorySupplier repositorySupplier;

    public TargetDefinitionContent(IProvisioningAgent agent, Collection<IInstallableUnit> units,
            Collection<URI> artifactRepositories, Collection<IArtifactRepository> additionalRepositories) {
        this.units = units;
        ArtifactRepositorySupplier uriRepositories = RepositoryArtifactProvider
                .createRepositoryLoader(artifactRepositories, agent);
        repositorySupplier = ArtifactRepositorySupplier.composite(uriRepositories,
                () -> Collections.unmodifiableCollection(additionalRepositories));

    }

    public Collection<IInstallableUnit> getUnits() {
        return units;
    }

    public ArtifactRepositorySupplier getArtifactRepositorySupplier() {
        return repositorySupplier;
    }
}
