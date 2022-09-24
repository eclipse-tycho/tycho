/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #462 - Delay Pom considered items to the final Target Platform calculation 
 *******************************************************************************/
package org.eclipse.tycho.target;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

@Mojo(name = "target-platform", threadSafe = true)
public class TargetPlatformMojo extends AbstractMojo {

    private static final String TARGET_PLATFORM_MOJO_EXECUTED = "TargetPlatformMojo.executed";

    // TODO site doc (including steps & parameters handled in afterProjectsRead?)
    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Component
    private TargetPlatformService platformService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        Object executed = reactorProject.getContextValue(TARGET_PLATFORM_MOJO_EXECUTED);
        if (executed != null) {
            //second execution should force recomputation
            platformService.clearTargetPlatform(reactorProject);
        } else {
            reactorProject.setContextValue(TARGET_PLATFORM_MOJO_EXECUTED, Boolean.TRUE);
        }
        //trigger target platform resoloution....
        platformService.getTargetPlatform(reactorProject);
    }

}
