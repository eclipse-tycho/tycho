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
package org.eclipse.tycho.p2.tools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.p2.repository.RepositoryBlackboardKey;

/**
 * List of p2 repositories for a p2 operation. Instances of this class store a list of metadata and
 * artifact repositories each, preserving the order in which the repositories were added.
 */
public final class RepositoryReferences {
    private final List<URI> metadataRepos = new ArrayList<>();

    private final List<URI> artifactRepos = new ArrayList<>();

    private TargetPlatform targetPlatform;

    /**
     * Adds the metadata repository at the given location.
     * 
     * @param metadataRepositoryLocation
     *            The folder containing the metadata repository file (<code>content.xml</code> or
     *            <code>content.jar</code>)
     */
    public void addMetadataRepository(File metadataRepositoryLocation) {
        metadataRepos.add(metadataRepositoryLocation.toURI());
    }

    /**
     * Adds the metadata repository at the given location.
     * 
     * @param metadataRepositoryLocation
     *            A URL pointing to a p2 metadata repository
     */
    public void addMetadataRepository(URI metadataRepository) {
        metadataRepos.add(metadataRepository);
    }

    /**
     * Adds a co-located metadata/artifact repository at the given location.
     * 
     * @param repository
     *            A URL pointing to a p2 repository
     */
    public void addRepository(URI repository) {
        addArtifactRepository(repository);
        addMetadataRepository(repository);
    }

    /**
     * Adds the artifact repository at the given location.
     * 
     * @param artifactRepositoryLocation
     *            The folder containing the artifact repository file structure
     */
    public void addArtifactRepository(File artifactRepositoryLocation) {
        artifactRepos.add(artifactRepositoryLocation.toURI());
    }

    /**
     * Adds the artifact repository at the given location.
     * 
     * @param artifactRepositoryLocation
     *            A URL pointing to a p2 artifact repository
     */
    public void addArtifactRepository(URI artifactRepository) {
        artifactRepos.add(artifactRepository);
    }

    /**
     * Adds the artifact repository which is stored in memory under the given key.
     * 
     * @param artifactRepositoryLocation
     *            A key identifying a repository registered on the artifact repository blackboard.
     */
    public void addArtifactRepository(RepositoryBlackboardKey blackboardKey) {
        artifactRepos.add(blackboardKey.toURI());
    }

    /**
     * Returns the list of metadata repositories in the order in which they were added.
     * 
     * @return the list metadata repositories.
     */
    public List<URI> getMetadataRepositories() {
        return Collections.unmodifiableList(metadataRepos);
    }

    /**
     * Returns the list of artifact repositories in the order in which they were added.
     * 
     * @return the list of artifact repositories.
     */
    public List<URI> getArtifactRepositories() {
        return Collections.unmodifiableList(artifactRepos);
    }

    public void setTargetPlatform(TargetPlatform targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public TargetPlatform getTargetPlatform() {
        return targetPlatform;
    }
}
