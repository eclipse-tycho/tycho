/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Issue #845 - Feature restrictions are not taken into account when using emptyVersion
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.target;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TargetPlatform;

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

    /**
     * Same as {@link #resolveArtifact(String, String, String)} but returning the result as
     * {@link IInstallableUnit}.
     * 
     * Note: "artifact" in "resolveArtifact" refers to a Tycho artifact, which technically represent
     * a p2 installable unit and optionally the associated p2 artifact.
     */
    IInstallableUnit resolveUnit(String type, String id, Version version)
            throws IllegalArtifactReferenceException, DependencyResolutionException;

    IInstallableUnit resolveUnit(String type, String id, VersionRange versionRange)
            throws IllegalArtifactReferenceException, DependencyResolutionException;

}
