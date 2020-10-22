/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
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

}
