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
package org.eclipse.tycho.core.legacy.mojos;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.resolver.LegacyLifecycleSupport;

/**
 * @goal after-projects-read-legacy
 */
public class AfterProjectsReadMojo extends AbstractMojo {

    /** @component */
    private LegacyLifecycleSupport legacyLifecycle;

    /** @parameter expression="${session}" */
    private MavenSession session;

    /** @parameter expression="${project}" */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        legacyLifecycle.setupProjects(session);
        legacyLifecycle.resolveProject(session, project);
    }

}
