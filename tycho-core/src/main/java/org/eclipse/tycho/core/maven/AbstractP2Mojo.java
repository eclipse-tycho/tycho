/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;
import org.eclipse.tycho.p2.tools.BuildContext;

public abstract class AbstractP2Mojo extends AbstractMojo {

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

    protected String getQualifier() {
        return qualifier;
    }

    protected List<TargetEnvironment> getEnvironments() {
        return TychoProjectUtils.getTargetPlatformConfiguration(getReactorProject()).getEnvironments();
    }

    protected BuildDirectory getBuildDirectory() {
        return getProjectIdentities().getBuildDirectory();
    }

    protected BuildContext getBuildContext() {
        return new BuildContext(getProjectIdentities(), getQualifier(), getEnvironments());
    }

}
