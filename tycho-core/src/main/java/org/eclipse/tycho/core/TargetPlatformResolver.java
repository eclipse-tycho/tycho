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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;

/**
 * Target platform content resolver. TODO This interface and its implementations require further
 * refinement. I need to decide if new resolver instance is required for each project.
 */
public interface TargetPlatformResolver {
    public void setupProjects(MavenSession session, MavenProject project, ReactorProject reactorProject);

    public TargetPlatform resolvePlatform(MavenSession session, MavenProject project,
            List<ReactorProject> reactorProjects, List<Dependency> dependencies);
}
