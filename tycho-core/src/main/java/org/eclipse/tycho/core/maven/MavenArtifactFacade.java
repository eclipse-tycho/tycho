/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - add toString/equals/hashCode
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public final class MavenArtifactFacade implements IArtifactFacade {

    private final Artifact wrappedArtifact;

    public MavenArtifactFacade(Artifact wrappedArtifact) {
        this.wrappedArtifact = wrappedArtifact;
    }

    @Override
    public File getLocation() {
        return wrappedArtifact.getFile();
    }

    @Override
    public String getGroupId() {
        return wrappedArtifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return wrappedArtifact.getArtifactId();
    }

    @Override
    public String getVersion() {
        // bug 352154: getVersion has expanded/non-expanded SNAPSHOT, depending on if the artifact is cached or available from remote 
        return wrappedArtifact.getBaseVersion();
    }

    @Override
    public String getPackagingType() {
        return wrappedArtifact.getType();
    }

    @Override
    public String getClassifier() {
        return wrappedArtifact.getClassifier();
    }

    @Override
    public List<String> getDependencyTrail() {
        List<String> trail = wrappedArtifact.getDependencyTrail();
        if (trail == null || trail.isEmpty()) {
            return Collections.singletonList(wrappedArtifact.getId());
        }
        return trail;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrappedArtifact);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenArtifactFacade other = (MavenArtifactFacade) obj;
        return Objects.equals(wrappedArtifact, other.wrappedArtifact);
    }

    @Override
    public String toString() {
        return "MavenArtifactFacade [wrappedArtifact=" + wrappedArtifact + "]";
    }

}
