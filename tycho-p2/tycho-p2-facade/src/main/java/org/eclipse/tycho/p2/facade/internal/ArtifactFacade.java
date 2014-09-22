/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class ArtifactFacade implements IArtifactFacade {

    private Artifact wrappedArtifact;

    public ArtifactFacade(Artifact wrappedArtifact) {
        this.wrappedArtifact = wrappedArtifact;
    }

    public File getLocation() {
        return wrappedArtifact.getFile();
    }

    public String getGroupId() {
        return wrappedArtifact.getGroupId();
    }

    public String getArtifactId() {
        return wrappedArtifact.getArtifactId();
    }

    public String getVersion() {
        // bug 352154: getVersion has expanded/non-expanded SNAPSHOT, depending on if the artifact is cached or available from remote 
        return wrappedArtifact.getBaseVersion();
    }

    public String getPackagingType() {
        return wrappedArtifact.getType();
    }

    public String getClassifier() {
        return wrappedArtifact.getClassifier();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((wrappedArtifact == null) ? 0 : wrappedArtifact.hashCode());
        return result;
    }

    /**
     * This is not a generated method. Watch out in case of modifications. Gives back true only if
     * both objects have a {@link DefaultArtifact} as {@link #wrappedArtifact}. The reason is, that
     * other {@link Artifact} implementations do not have correct {@link Object#equals(Object)} and
     * {@link Object#hashCode()} implementations.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactFacade other = (ArtifactFacade) obj;

        // THIS two line is not generated. 
        if (!(wrappedArtifact instanceof DefaultArtifact) || !(other.wrappedArtifact instanceof DefaultArtifact))
            return false;

        if (wrappedArtifact == null) {
            if (other.wrappedArtifact != null)
                return false;
        } else if (!wrappedArtifact.equals(other.wrappedArtifact))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ArtifactFacade [wrappedArtifact=" + wrappedArtifact + "]";
    }
}
