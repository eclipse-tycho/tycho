/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.adapters;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public class MavenReactorProjectIdentities extends ReactorProjectIdentities {

    private final MavenProject project;

    public MavenReactorProjectIdentities(MavenProject project) {
        this.project = project;
    }

    @Override
    public String getGroupId() {
        return project.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return project.getArtifactId();
    }

    @Override
    public String getVersion() {
        return project.getVersion();
    }

    @Override
    public File getBasedir() {
        return project.getBasedir();
    }

    @Override
    public BuildDirectory getBuildDirectory() {
        return DefaultReactorProject.adapt(project).getBuildDirectory();
    }

}
