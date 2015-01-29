/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;
import org.eclipse.tycho.p2.tools.BuildContext;

public abstract class AbstractRepositoryMojo extends AbstractMojo {

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "buildQualifier", readonly = true)
    private String qualifier;

    protected MavenProject getProject() {
        return project;
    }

    protected ReactorProject getReactorProject() {
        return DefaultReactorProject.adapt(project);
    }

    protected ReactorProjectIdentities getProjectIdentities() {
        return new MavenReactorProjectIdentities(project);
    }

    protected MavenSession getSession() {
        return session;
    }

    protected BuildOutputDirectory getBuildDirectory() {
        return getProjectIdentities().getBuildDirectory();
    }

    protected BuildContext getBuildContext() {
        List<TargetEnvironment> environments = TychoProjectUtils.getTargetPlatformConfiguration(project)
                .getEnvironments();
        return new BuildContext(getProjectIdentities(), qualifier, environments);
    }

    protected File getAssemblyRepositoryLocation() {
        return getBuildDirectory().getChild("repository");
    }

}
