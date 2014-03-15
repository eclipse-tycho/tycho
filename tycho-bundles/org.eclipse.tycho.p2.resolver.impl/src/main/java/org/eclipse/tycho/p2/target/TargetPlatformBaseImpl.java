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
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

abstract class TargetPlatformBaseImpl implements P2TargetPlatform {

    // content

    /**
     * All installable units contained in the target platform. This includes reactor-external
     * content and all results of upstream reactor projects (or all projects in case of the
     * preliminary target platform where the reactor build order isn't known yet). Configured and
     * automatic filters have been applied.
     */
    private final LinkedHashSet<IInstallableUnit> installableUnits;

    // reverse lookup from target platform content to the contributing artifact/project 

    /**
     * Map from installable units back to the contributing reactor project. Note: May contain
     * installable units as keys which are not part of the target platform.
     */
    private final Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup;

    /**
     * Map from installable units back to the contributing artifacts. Note: May contain installable
     * units as keys which are not part of the target platform.
     */
    final Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup;

    // additional information on the dependency resolution context

    /**
     * Execution environment information with information about the packages provided by the JRE.
     */
    final ExecutionEnvironmentResolutionHints executionEnvironment;

    final IRawArtifactFileProvider jointArtifacts;
    @Deprecated
    private LocalArtifactRepository localArtifactRepository;

    public TargetPlatformBaseImpl(LinkedHashSet<IInstallableUnit> installableUnits,
            ExecutionEnvironmentResolutionHints executionEnvironment, IRawArtifactFileProvider jointArtifacts,
            LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup) {
        this.installableUnits = installableUnits;
        this.executionEnvironment = executionEnvironment;
        this.reactorProjectLookup = reactorProjectLookup;
        this.mavenArtifactLookup = mavenArtifactLookup;
        this.jointArtifacts = jointArtifacts;
        this.localArtifactRepository = localArtifactRepository;
    }

    public Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    public final ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return executionEnvironment;
    }

    public Map<IInstallableUnit, ReactorProjectIdentities> getOriginalReactorProjectMap() {
        return reactorProjectLookup;
    }

    public final Map<IInstallableUnit, IArtifactFacade> getOriginalMavenArtifactMap() {
        return mavenArtifactLookup;
    }

    public final File getLocalArtifactFile(IArtifactKey key) {
        return jointArtifacts.getArtifactFile(key);
    }

    public final void saveLocalMavenRepository() {
        localArtifactRepository.save();
    }

}
