/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.resolver.LegacyLifecycleSupport;
import org.eclipse.tycho.resolver.TychoDependencyResolver;

@Component(role = LegacyLifecycleSupport.class)
public class DefaultLegacyLifecycleSupport implements LegacyLifecycleSupport {

    @Requirement
    private TychoDependencyResolver resolver;

    public void afterProjectsRead(MavenSession session) {
        setupProjects(session);

        List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            resolveProject(session, project);
        }
    }

    public void setupProjects(MavenSession session) {
        List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
        }
    }

    public void resolveProject(MavenSession session, MavenProject project) {
        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        resolver.resolveProject(session, project, reactorProjects);
    }

}
