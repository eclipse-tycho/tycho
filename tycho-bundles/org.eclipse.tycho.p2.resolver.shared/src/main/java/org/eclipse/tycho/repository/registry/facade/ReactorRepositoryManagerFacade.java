/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import java.util.List;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

/**
 * Manages the p2 repositories for the projects' build results ("publishing repository") and the p2
 * repositories with the projects' context artifacts ("target platform").
 */
public interface ReactorRepositoryManagerFacade {

    /**
     * Computes the target platform with dependency-only p2 metadata and attaches it to the given
     * project.
     * 
     * @param project
     *            the reactor project to compute the target platform for.
     * @param resolvedEnvironment
     */
    TargetPlatform computePreliminaryTargetPlatform(ReactorProject project,
            TargetPlatformConfigurationStub tpConfiguration, ExecutionEnvironmentConfiguration eeConfiguration,
            List<ReactorProject> reactorProjects);

    /**
     * Computes the (immutable) target platform with final p2 metadata and attaches it to the given
     * project.
     * 
     * @param project
     *            the reactor project to compute the target platform for.
     * @param upstreamProjects
     *            Other projects in the reactor which have already been built and may be referenced
     *            by the given project.
     */
    void computeFinalTargetPlatform(ReactorProject project, List<? extends ReactorProjectIdentities> upstreamProjects,
            PomDependencyCollector pomDependencyCollector);

    /**
     * Returns the target platform with final p2 metadata for the given project.
     */
    TargetPlatform getFinalTargetPlatform(ReactorProject project);

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            a reference to a project in the reactor.
     */
    PublishingRepositoryFacade getPublishingRepository(ReactorProjectIdentities project);

}
