/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;

/**
 * Target platform content resolver. TODO This interface and its implementations require further
 * refinement. I need to decide if new resolver instance is required for each project.
 */
// TODO 364134 revise name
public interface TargetPlatformResolver {
    public void setupProjects(MavenSession session, MavenProject project, ReactorProject reactorProject);

    public TargetPlatform computeTargetPlatform(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects);

    public DependencyArtifacts resolveDependencies(MavenSession session, MavenProject project,
            TargetPlatform resolutionContext, List<ReactorProject> reactorProjects,
            DependencyResolverConfiguration resolverConfiguration);

    public void injectDependenciesIntoMavenModel(MavenProject project, AbstractTychoProject projectType,
            DependencyArtifacts resolvedDependencies, Logger logger);
}
