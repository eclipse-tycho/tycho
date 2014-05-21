/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;
import org.eclipse.tycho.p2.tools.BuildContext;

// TODO share between Maven plug-ins?
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

    protected ReactorProjectIdentities getProjectIdentities() {
        return new MavenReactorProjectIdentities(project);
    }

    protected MavenSession getSession() {
        return session;
    }

    protected String getQualifier() {
        return qualifier;
    }

    protected BuildOutputDirectory getBuildDirectory() {
        return getProjectIdentities().getBuildDirectory();
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
        List<TargetEnvironment> environments = TychoProjectUtils.getTargetPlatformConfiguration(project)
                .getEnvironments();
        return new BuildContext(getProjectIdentities(), getQualifier(), environments);
    }
}
