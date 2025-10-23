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
 *    Christoph Läubrich - Bug 532575
 *    Christoph Läubrich - Issue #460 - Delay classpath resolution to the compile phase 
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.resolver.TychoResolver;

@Named
@Singleton
public class DefaultTychoResolver implements TychoResolver {

    private static final String SETUP_MARKER = "DefaultTychoResolver/Setup";
    private static final String RESOLVE_MARKER = "DefaultTychoResolver/Resolve";
    private static final String TYCHO_ENV_OSGI_WS = "tycho.env.osgi.ws";
    private static final String TYCHO_ENV_OSGI_OS = "tycho.env.osgi.os";
    private static final String TYCHO_ENV_OSGI_ARCH = "tycho.env.osgi.arch";

    @Inject
    private Logger logger;

    @Inject
    @Named("p2")
    private DependencyResolver dependencyResolver;

    @Inject
    TychoProjectManager projectManager;

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        //This will bootstrap the project and init it with the session
        ReactorProject reactorProject = DefaultReactorProject.adapt(project, session);
        synchronized (reactorProject) {
            TychoProject tychoProject = projectManager.getTychoProject(project).orElse(null);
            if (tychoProject instanceof AbstractTychoProject dr) {
                if (reactorProject.getContextValue(SETUP_MARKER) != null) {
                    return;
                }
                reactorProject.setContextValue(SETUP_MARKER, true);

                //enhance the current project properties with the currently used environment
                Properties properties = EquinoxResolver.computeMergedProperties(project, session);
                String arch = PlatformPropertiesUtils.getArch(properties);
                String os = PlatformPropertiesUtils.getOS(properties);
                String ws = PlatformPropertiesUtils.getWS(properties);
                Properties projectProperties = project.getProperties();
                projectProperties.put(TYCHO_ENV_OSGI_WS, ws);
                projectProperties.put(TYCHO_ENV_OSGI_OS, os);
                projectProperties.put(TYCHO_ENV_OSGI_ARCH, arch);

                //FIXME this should actually happen lazy on first access so we do not require more here than bootstrap the project with a session above 
                dr.setupProject(session, project);
                dependencyResolver.setupProjects(session, project, reactorProject);
            }
        }
    }

    @Override
    public void resolveProject(MavenSession mavenSession, MavenProject mavenProject) {
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(mavenProject);
        if (tychoProject.isEmpty()) {
            return;
        }
        ReactorProject reactorProject = DefaultReactorProject.adapt(mavenProject);
        synchronized (reactorProject) {
            if (reactorProject.getContextValue(RESOLVE_MARKER) != null) {
                return;
            }
            reactorProject.setContextValue(RESOLVE_MARKER, true);
            TychoProject project = tychoProject.get();
            DependencyArtifacts dependencyArtifacts = project.getDependencyArtifacts(reactorProject);
            DependencyArtifacts testDependencyArtifacts = project.getTestDependencyArtifacts(reactorProject);
            dependencyResolver.injectDependenciesIntoMavenModel(mavenProject, project, dependencyArtifacts,
                    testDependencyArtifacts, logger);
            if (logger.isDebugEnabled() && DebugUtils.isDebugEnabled(mavenSession, mavenProject)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Injected dependencies for ").append(mavenProject.toString()).append("\n");
                for (Dependency dependency : mavenProject.getDependencies()) {
                    sb.append("  ").append(dependency.toString());
                }
                logger.debug(sb.toString());
            }
        }
    }
}
