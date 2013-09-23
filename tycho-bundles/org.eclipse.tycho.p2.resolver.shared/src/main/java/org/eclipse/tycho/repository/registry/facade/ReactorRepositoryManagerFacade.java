/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import java.util.List;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

// TODO summarize purpose of this service
public interface ReactorRepositoryManagerFacade {

    /**
     * Computes the target platform with dependency-only p2 metadata and attaches it to the given
     * project.
     * 
     * @param project
     *            the reactor project to compute the target platform for.
     */
    TargetPlatform computePreliminaryTargetPlatform(ReactorProject project,
            TargetPlatformConfigurationStub tpConfiguration, ExecutionEnvironmentConfiguration eeConfiguration,
            List<IReactorArtifactFacade> reactorProjects, PomDependencyCollector pomDependencies);

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
    // TODO return target platform?
    void computeFinalTargetPlatform(ReactorProject project, List<? extends ReactorProjectCoordinates> upstreamProjects);

    // TODO add method to get (final) target platform?

    /**
     * Returns the project's publishing repository.
     * 
     * @param project
     *            the coordinates of a project in the reactor.
     */
    PublishingRepositoryFacade getPublishingRepository(ReactorProjectCoordinates project);

}
