/*******************************************************************************
 * Copyright (c) 2013, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

public class FinalTargetPlatformImpl extends TargetPlatformBaseImpl {

    private final Set<IInstallableUnit> installableUnits;
    private final Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup;

    public FinalTargetPlatformImpl(Set<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup) {
        super(executionEnvironment, jointArtifacts, localArtifactRepository, mavenArtifactLookup);
        this.installableUnits = installableUnits;
        this.reactorProjectLookup = reactorProjectLookup;
    }

    public Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    public void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits) {
        // not needed; already done during dependency resolution with the preliminary TP
    }

    public Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap() {
        return reactorProjectLookup;
    }

}
