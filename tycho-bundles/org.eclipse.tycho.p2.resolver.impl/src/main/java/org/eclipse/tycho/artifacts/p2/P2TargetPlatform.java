/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts.p2;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

/**
 * Specialization of the {@link TargetPlatform} interface for a target platform which includes p2
 * metadata.
 */
public interface P2TargetPlatform extends TargetPlatform {

    Set<IInstallableUnit> getInstallableUnits();

    /**
     * Returns additional information for resolving against the configured execution environment.
     */
    ExecutionEnvironmentResolutionHints getEEResolutionHints();

    void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits);

    File getLocalArtifactFile(IArtifactKey key);

    /**
     * Returns the reactor project which contributed this installable unit to the target platform.
     * 
     * @param iu
     *            An installable unit from the target platform.
     */
    ReactorProjectIdentities lookUpOriginalReactorProject(IInstallableUnit iu);

    /**
     * Returns the Maven artifact which contributed this installable unit to the target platform.
     * 
     * @param iu
     *            An installable unit from the target platform.
     */
    // TODO make this method include the artifacts from the local Maven repository
    IArtifactFacade lookUpOriginalMavenArtifact(IInstallableUnit iu);

    // TODO 393004 this method should not be necessary
    void saveLocalMavenRepository();

}
