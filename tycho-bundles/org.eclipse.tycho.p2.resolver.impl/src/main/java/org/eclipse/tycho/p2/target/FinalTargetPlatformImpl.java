/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

public class FinalTargetPlatformImpl implements P2TargetPlatform {

    private Set<IInstallableUnit> installableUnits;

    public FinalTargetPlatformImpl(LinkedHashSet<IInstallableUnit> installableUnits) {
        this.installableUnits = installableUnits;
    }

    public Collection<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    // TODO 412416 implement for resolver
    public ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        throw new UnsupportedOperationException();
    }

    public void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits) {
        // not needed; already done during dependency resolution with the preliminary TP
    }

    public File getLocalArtifactFile(IArtifactKey key) {
        // TODO 412416 implement
        throw new UnsupportedOperationException();
    }

    public void saveLocalMavenRepository() {
        // TODO 412416 implement
        throw new UnsupportedOperationException();
    }

    public IArtifactFacade getMavenArtifact(IInstallableUnit iu) {
        throw new UnsupportedOperationException();
    }

    // TODO 412416 remove from interface
    public Collection<IInstallableUnit> getReactorProjectIUs(File projectLocation, boolean primary) {
        throw new UnsupportedOperationException();
    }

}
