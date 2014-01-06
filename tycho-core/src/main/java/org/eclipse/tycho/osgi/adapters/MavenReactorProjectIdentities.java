/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.adapters;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectIdentities;

public class MavenReactorProjectIdentities implements ReactorProjectIdentities {

    private final MavenProject project;

    // derived members
    private final BuildOutputDirectory targetFolder;

    public MavenReactorProjectIdentities(MavenProject project) {
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

    public File getBasedir() {
        return project.getBasedir();
    }

    public BuildOutputDirectory getBuildDirectory() {
        return targetFolder;
    }

}
