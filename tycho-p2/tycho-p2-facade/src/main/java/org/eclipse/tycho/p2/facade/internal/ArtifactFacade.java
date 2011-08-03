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

    public String getClassidier() {
        return wrappedArtifact.getClassifier();
    }

}
