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
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.resolver.TychoDependencyResolver;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener")
public class TychoMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private TychoDependencyResolver resolver;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (disableLifecycleParticipation(session)) {
            return;
        }
        configureComponents(session);

        List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
        }

        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        for (MavenProject project : projects) {
            resolver.resolveProject(session, project, reactorProjects);
        }
    }

    private boolean disableLifecycleParticipation(MavenSession session) {
        // command line property to disable Tycho lifecycle participant
        if ("maven".equals(session.getUserProperties().get("tycho.mode"))) {
            return true;
        }
        if (session.getUserProperties().containsKey("m2e.version")) {
            return true;
        }
        return false;
    }

    private void configureComponents(MavenSession session) {
        // TODO why does the bundle reader need to cache stuff in the local maven repository?
        File localRepository = new File(session.getLocalRepository().getBasedir());
        ((DefaultBundleReader) bundleReader).setLocationRepository(localRepository);
    }

}
