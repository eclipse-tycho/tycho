/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class AttachedArtifact implements IArtifactFacade {

    private final MavenProject project;

    private final File location;

    private final String classifier;

    public AttachedArtifact(MavenProject project, File location, String classifier) {
        this.project = project;
        this.location = location;
        this.classifier = classifier;
    }

    public File getLocation() {
        return location;
    }

    public String getGroupId() {
        return project.getGroupId();
    }

    public String getArtifactId() {
        return project.getArtifactId();
    }

    public String getClassidier() {
        return classifier;
    }

    public String getVersion() {
        return project.getVersion();
    }

    public String getPackagingType() {
        return project.getPackaging();
    }

}
