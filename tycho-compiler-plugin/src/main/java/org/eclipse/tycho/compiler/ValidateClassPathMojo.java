/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;

/**
 * This mojo could be added to a build if validation of the classpath is desired before the
 * compile-phase.
 */
@Mojo(name = "validate-classpath", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ValidateClassPathMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        TychoProject projectType = projectTypes.get(project.getPackaging());
        if (projectType instanceof OsgiBundleProject bundleProject) {
            ReactorProject reactorProject = DefaultReactorProject.adapt(project);
            if (reactorProject.getContextValue(TychoConstants.CTX_DEPENDENCY_ARTIFACTS) != null) {
                bundleProject.getClasspath(reactorProject);
            } else {
                getLog().info("Skipped classpath validation as project is currently not resolved");
            }
        }
    }

}
