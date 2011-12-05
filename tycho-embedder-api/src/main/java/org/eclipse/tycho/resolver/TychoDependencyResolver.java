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
package org.eclipse.tycho.resolver;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;

// TODO 364134 split target platform computation and dependency resolution 
public interface TychoDependencyResolver {
    public void setupProject(MavenSession session, MavenProject project, ReactorProject reactorProject);

    public void resolveProject(MavenSession session, MavenProject project, List<ReactorProject> reactorProjects);

    public void traverse(MavenProject project, DependencyVisitor visitor);
}
