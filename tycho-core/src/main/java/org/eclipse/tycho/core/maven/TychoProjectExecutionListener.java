/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.resolver.TychoResolver;

@Component(role = ProjectExecutionListener.class, hint = "tycho")
public class TychoProjectExecutionListener implements ProjectExecutionListener {

    @Requirement
    private TychoResolver resolver;

    @Requirement
    private ModelWriter modelWriter;

    @Requirement
    private LegacySupport legacySupport;

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {

    }

    private boolean requiresDependencies(ProjectExecutionEvent event) {
        List<MojoExecution> executionPlan = event.getExecutionPlan();
        if (executionPlan == null) {
            //we can't know ...
            return true;
        }
        for (MojoExecution execution : executionPlan) {
            MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();
            String dependencyResolutionRequired = mojoDescriptor.getDependencyResolutionRequired();
            if (dependencyResolutionRequired != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) throws LifecycleExecutionException {
        if (TychoConstants.USE_OLD_RESOLVER || !requiresDependencies(event)) {
            return;
        }
        MavenProject mavenProject = event.getProject();
        MavenSession mavenSession = event.getSession();
        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(mavenSession);
        MavenSession oldSession = legacySupport.getSession();
        try {
            legacySupport.setSession(mavenSession);
            resolver.resolveProject(mavenSession, mavenProject, reactorProjects);
        } finally {
            legacySupport.setSession(oldSession);
        }
        if (TychoMavenLifecycleParticipant.DUMP_DATA) {
            try {
                modelWriter.write(new File(mavenProject.getBasedir(), "pom-model-final.xml"), Map.of(),
                        mavenProject.getModel());
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) throws LifecycleExecutionException {
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
    }

}
