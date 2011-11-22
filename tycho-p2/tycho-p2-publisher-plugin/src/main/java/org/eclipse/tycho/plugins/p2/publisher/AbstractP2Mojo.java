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
package org.eclipse.tycho.plugins.p2.publisher;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.TargetEnvironment;

// TODO share between Maven plug-ins?
public abstract class AbstractP2Mojo extends AbstractMojo {

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

    protected String getQualifier() {
        return qualifier;
    }

    protected BuildOutputDirectory getBuildDirectory() {
        return new BuildOutputDirectory(getProject().getBuild().getDirectory());
    }

    protected EclipseRepositoryProject getEclipseRepositoryProject() {
        return (EclipseRepositoryProject) getTychoProjectFacet(ArtifactKey.TYPE_ECLIPSE_REPOSITORY);
    }

    private TychoProject getTychoProjectFacet(String packaging) {
        TychoProject facet;
        try {
            facet = (TychoProject) session.lookup(TychoProject.class.getName(), packaging);
        } catch (ComponentLookupException e) {
            throw new IllegalStateException("Could not lookup required component", e);
        }
        return facet;
    }

    protected BuildContext getBuildContext() {
        return new BuildContext(getQualifier(), getEnvironmentsForFacade(), getBuildDirectory());
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
