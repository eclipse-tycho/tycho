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
package org.eclipse.tycho.p2.facade.internal;

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

    public String getClassifier() {
        return classifier;
    }

    public String getVersion() {
        return project.getVersion();
    }

    public String getPackagingType() {
        return project.getPackaging();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AttachedArtifact other = (AttachedArtifact) obj;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (project == null) {
            if (other.project != null)
                return false;
        } else if (!project.equals(other.project))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AttachedArtifact [project=" + project + ", location=" + location + ", classifier=" + classifier + "]";
    }

}
