/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.TargetEnvironment;

public abstract class AbstractRepositoryMojo extends AbstractMojo {
    /** @parameter expression="${session}" */
    private MavenSession session;

    /** @parameter expression="${project}" */
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

    protected MavenSession getSession() {
        return session;
    }

    protected BuildOutputDirectory getBuildDirectory() {
        return new BuildOutputDirectory(getProject().getBuild().getDirectory());
    }

    protected BuildContext getBuildContext() {
        return new BuildContext(qualifier, getEnvironmentsForFacade(), getBuildDirectory());
    }

    protected File getAssemblyRepositoryLocation() {
        return getBuildDirectory().getChild("repository");
    }

    /**
     * Returns the configured environments in a format suitable for the p2 tools facade.
     */
    private List<TargetEnvironment> getEnvironmentsForFacade() {
        // TODO use shared class everywhere?

        List<org.eclipse.tycho.core.TargetEnvironment> original = TychoProjectUtils.getTargetPlatformConfiguration(
                project).getEnvironments();
        List<TargetEnvironment> converted = new ArrayList<TargetEnvironment>(original.size());
        for (org.eclipse.tycho.core.TargetEnvironment env : original) {
            converted.add(new TargetEnvironment(env.getWs(), env.getOs(), env.getArch()));
        }
        return converted;
    }
}
