/*******************************************************************************
 * Copyright (c) 2013, 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;
import org.eclipse.tycho.repository.p2base.metadata.ImmutableInMemoryMetadataRepository;

public class FinalTargetPlatformImpl extends TargetPlatformBaseImpl {

    public FinalTargetPlatformImpl(LinkedHashSet<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup) {
        super(installableUnits, executionEnvironment, jointArtifacts, localArtifactRepository, reactorProjectLookup,
                mavenArtifactLookup);
    }

    @Override
    public void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits) {
        // not needed; already done during dependency resolution with the preliminary TP
    }

    @Override
    public IMetadataRepository getInstallableUnitsAsMetadataRepository() {
        return new ImmutableInMemoryMetadataRepository(installableUnits);
    }

    @Override
    public File getArtifactLocation(org.eclipse.tycho.ArtifactKey artifact) {
        IArtifactKey p2Artifact = ArtifactTypeHelper.toP2ArtifactKey(artifact);
        if (p2Artifact != null) {
            return artifacts.getArtifactFile(p2Artifact);
        }
        return null;
    }

}
