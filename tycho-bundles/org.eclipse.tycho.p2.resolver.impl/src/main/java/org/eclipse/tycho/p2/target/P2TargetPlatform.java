/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP SE and others.
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
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;

/**
 * Extension of the {@link TargetPlatform} interface by methods which depend on p2.
 */
public interface P2TargetPlatform extends TargetPlatform {

    Set<IInstallableUnit> getInstallableUnits();

    /**
     * Returns the target platform content as (immutable) p2 metadata repository.
     */
    IMetadataRepository getInstallableUnitsAsMetadataRepository();

    /**
     * Returns additional information for resolving against the configured execution environment.
     */
    ExecutionEnvironmentResolutionHints getEEResolutionHints();

    void reportUsedLocalIUs(Collection<IInstallableUnit> usedUnits);

    File getLocalArtifactFile(IArtifactKey key);

    /**
     * Returns the map from target platform installable units back to the contributing reactor
     * project.
     * 
     * <p>
     * Note: The map may contain additional installable units as keys, i.e. not all keys are
     * necessarily part of the target platform.
     * </p>
     */
    Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap();

    /**
     * Returns the map from target platform installable units back to the contributing Maven
     * artifacts.
     * 
     * <p>
     * Note: The map may contain additional installable units as keys, i.e. not all keys are
     * necessarily part of the target platform.
     * </p>
     */
    // TODO make this method include the artifacts from the local Maven repository?
    Map<IInstallableUnit, IArtifactFacade> getOriginalMavenArtifactMap();

    // TODO 393004 this method should not be necessary
    void saveLocalMavenRepository();

}
