/*******************************************************************************
 * Copyright (c) 2013, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IRawArtifactFileProvider;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.repository.ImmutableInMemoryMetadataRepository;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;

public class FinalTargetPlatformImpl extends TargetPlatformBaseImpl {

    private IArtifactRepository artifactRepository;
    private IMetadataRepository metadataRepository;

    public FinalTargetPlatformImpl(LinkedHashSet<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository, Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup,
            IArtifactRepository artifactRepository, Set<IInstallableUnit> shadowed) {
        super(installableUnits, executionEnvironment, jointArtifacts, localArtifactRepository, reactorProjectLookup,
                mavenArtifactLookup, shadowed);
        this.artifactRepository = artifactRepository;
        this.metadataRepository = new ImmutableInMemoryMetadataRepository(installableUnits, false);
    }

    @Override
    public void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits) {
        // not needed; already done during dependency resolution with the preliminary TP
    }

    @Override
    public IMetadataRepository getMetadataRepository() {
        return metadataRepository;
    }

    @Override
    public IArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

}
