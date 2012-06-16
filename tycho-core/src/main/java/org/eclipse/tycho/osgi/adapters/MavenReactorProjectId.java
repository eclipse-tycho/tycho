/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.adapters;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectId;

public class MavenReactorProjectId implements ReactorProjectId {

    private final MavenProject project;

    // derived members
    private final BuildOutputDirectory targetFolder;

    public MavenReactorProjectId(MavenProject project) {
        this.project = project;
        this.targetFolder = new BuildOutputDirectory(project.getBuild().getDirectory());
    }

    public String getGroupId() {
        return project.getGroupId();
    }

    public String getArtifactId() {
        return project.getArtifactId();
    }

    public String getVersion() {
        return project.getVersion();
    }

    public BuildOutputDirectory getBuildDirectory() {
        return targetFolder;
    }

}
