/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectCoordinates;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectCoordinates;
import org.eclipse.tycho.p2.tools.BuildContext;

public abstract class AbstractRepositoryMojo extends AbstractMojo {
    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     * 
     * @parameter expression="${buildQualifier}"
     */
    private String qualifier;

    protected MavenProject getProject() {
        return project;
    }

    protected ReactorProjectCoordinates getProjectCoordinates() {
        return new MavenReactorProjectCoordinates(project);
    }

    protected MavenSession getSession() {
        return session;
    }

    protected BuildOutputDirectory getBuildDirectory() {
        return getProjectCoordinates().getBuildDirectory();
    }

    protected BuildContext getBuildContext() {
        List<TargetEnvironment> environments = TychoProjectUtils.getTargetPlatformConfiguration(project).getEnvironments();
        return new BuildContext(getProjectCoordinates(), qualifier, environments);
    }
    protected File getAssemblyRepositoryLocation() {
        return getBuildDirectory().getChild("repository");
    }


}
