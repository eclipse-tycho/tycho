/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public abstract class AbstractVersionMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "project.packaging", required = true, readonly = true)
    protected String packaging;

    @Component(role = TychoProject.class)
    protected Map<String, TychoProject> projectTypes;

    protected String getOSGiVersion() {
        ArtifactKey osgiArtifact = getOSGiArtifact();
        return osgiArtifact != null ? osgiArtifact.getVersion() : null;
    }

    protected String getOSGiId() {
        ArtifactKey osgiArtifact = getOSGiArtifact();
        return osgiArtifact != null ? osgiArtifact.getId() : null;
    }

    private ArtifactKey getOSGiArtifact() {
        TychoProject projectType = projectTypes.get(packaging);
        if (projectType == null) {
            return null;
        }
        return projectType.getArtifactKey(DefaultReactorProject.adapt(project));
    }

}
