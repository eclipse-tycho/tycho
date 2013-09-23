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
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactFileProvider;

abstract class TargetPlatformBaseImpl implements P2TargetPlatform {

    // content -> see sub-types

    // reverse lookup from target platform content to the contributing artifact/project 

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

    public TargetPlatformBaseImpl(ExecutionEnvironmentResolutionHints executionEnvironment,
            IRawArtifactFileProvider jointArtifacts, LocalArtifactRepository localArtifactRepository,
            Map<IInstallableUnit, IArtifactFacade> mavenArtifactLookup) {
        this.executionEnvironment = executionEnvironment;
        this.mavenArtifactLookup = mavenArtifactLookup;
        this.jointArtifacts = jointArtifacts;
        this.localArtifactRepository = localArtifactRepository;
    }

    public final ExecutionEnvironmentResolutionHints getEEResolutionHints() {
        return executionEnvironment;
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
