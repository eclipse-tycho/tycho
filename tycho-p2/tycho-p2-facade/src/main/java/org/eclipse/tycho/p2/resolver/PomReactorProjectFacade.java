/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.p2.metadata.ReactorProjectFacade;

final class PomReactorProjectFacade implements ReactorProjectFacade {

    private ReactorProject project;
    private Artifact artifact;

    public PomReactorProjectFacade(Artifact artifact, ReactorProject project) {
        this.artifact = artifact;
        this.project = project;
    }

    @Override
    public File getLocation() {
        return artifact.getFile();
    }

    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public String getVersion() {
        return artifact.getBaseVersion();
    }

    @Override
    public String getPackagingType() {
        return artifact.getType();
    }

    @Override
    public ReactorProject getReactorProject() {
        return project;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, project);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PomReactorProjectFacade other = (PomReactorProjectFacade) obj;
        return Objects.equals(artifact, other.artifact) && Objects.equals(project, other.project);
    }

}
