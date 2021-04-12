/*******************************************************************************
 * Copyright (c) 2011, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - implement ReactorProjectFacade, add hashCode/equals/toString
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.io.File;
import java.util.Objects;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;

public final class AttachedArtifact implements ReactorProjectFacade {

    private final MavenProject project;

    private final File location;

    private final String classifier;

    public AttachedArtifact(MavenProject project, File location, String classifier) {
        this.project = project;
        this.location = location;
        this.classifier = classifier;
    }

    @Override
    public File getLocation() {
        return location;
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
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getVersion() {
        return project.getVersion();
    }

    @Override
    public String getPackagingType() {
        return project.getPackaging();
    }

    @Override
    public ReactorProject getReactorProject() {
        return DefaultReactorProject.adapt(project);
    }

    @Override
    public String toString() {
        return "AttachedArtifact [project=" + project + ", location=" + location + ", classifier=" + classifier + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(classifier, location, project);
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
        return Objects.equals(classifier, other.classifier) && Objects.equals(location, other.location)
                && Objects.equals(project, other.project);
    }

}
