/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.test.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.repository.AbstractRepositoryReader;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryReader;

public class LocalRepositoryStub {
    Map<MavenCoordinates, File> artifacts = new HashMap<MavenCoordinates, File>();

    /**
     * Logically add an artifact to the repository stub.
     */
    public void addArtifact(IArtifactFacade artifact) {
        artifacts.put(new MavenCoordinates(artifact), artifact.getLocation());
    }

    /**
     * Returns the provider for all artifacts in to the repository stub. Note: The provider is
     * linked to the repository, and hence is affected by subsequent modifications to the repository
     * (see {@link #addArtifact(IArtifactFacade)}).
     */
    public RepositoryReader getArtifactProvider() {
        return new ArtifactProviderStub();
    }

    private class ArtifactProviderStub extends AbstractRepositoryReader {

        public File getLocalArtifactLocation(GAV gav, String classifier, String extension) throws IOException {
            MavenCoordinates coordinates = new MavenCoordinates(gav, classifier, extension);
            File artifact = artifacts.get(coordinates);
            if (artifact == null)
                throw new IllegalStateException("Test artifact does not exist: " + coordinates);
            return artifact;
        }

    }

}
