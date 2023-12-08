/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #462 - Delay Pom considered items to the final Target Platform calculation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;

/**
 * Resolves project dependencies against the content of the target platform.
 */
public interface DependencyResolver {

    public void setupProjects(MavenSession session, MavenProject project, ReactorProject reactorProject);

    PomDependencyCollector resolvePomDependencies(MavenSession session, MavenProject project);

    public TargetPlatform computePreliminaryTargetPlatform(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects);

    /**
     * @param targetPlatform
     *            The candidate artifacts which may be used to resolve dependencies. If
     *            <code>null</code>, the final target platform of the project will be used.
     */
    public DependencyArtifacts resolveDependencies(MavenSession session, MavenProject project,
            TargetPlatform targetPlatform, List<ReactorProject> reactorProjects,
            DependencyResolverConfiguration resolverConfiguration, List<TargetEnvironment> environments);

    public void injectDependenciesIntoMavenModel(MavenProject project, TychoProject projectType,
            DependencyArtifacts resolvedDependencies, DependencyArtifacts testDepedencyArtifacts, Logger logger);
}
