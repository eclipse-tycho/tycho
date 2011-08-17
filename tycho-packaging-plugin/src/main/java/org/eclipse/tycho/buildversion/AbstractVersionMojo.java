/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public abstract class AbstractVersionMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
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
