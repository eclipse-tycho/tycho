/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An artifact (i.e. a file) in project build target platform.
 */
public interface ArtifactDescriptor {
    /**
     * Eclipse/OSGi artifact key (a.k.a. "coordinates") that uniquely identify the artifact
     */
    public ArtifactKey getKey();

    /**
     * Artifact location on local filesystem
     * 
     * @param fetch
     *            whether to fetch artifact if not already available locally
     * @return the artifact location if already available or if <code>fetch=true</code> and fetching
     *         succeds; <code>null</code> otherwise.
     */
    @Deprecated
    default File getLocation(boolean fetch) {
        Optional<File> location = getLocation();
        if (location.isPresent()) {
            return location.get();
        }
        if (fetch) {
            try {
                return fetchArtifact().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                //do nothing... thats actually bad but it seems sometimes desired!
            }
        }
        return null;
    }

    /**
     * Gets the location of the artifact if already know, if you really like to fetch the artifact
     * and make is available use {@link #fetchArtifact()} instead.
     * 
     * @return an empty optional if the file is not currently available or an optional describing
     *         the location of that file
     */
    Optional<File> getLocation();

    /**
     * Fetch the artifact in a possibly asynchronous way, that is returning a
     * {@link CompletableFuture} that is either already completed (e.g. because the file is already
     * available), failed (e.g because the fetching of the file itself failed) or will be complete
     * sometime later on when the file is ready to be accessed.
     * 
     * @return a {@link CompletableFuture} that represents the current state of fetching this file
     */
    CompletableFuture<File> fetchArtifact();

    /**
     * ReactorProject corresponding to the artifact or null if the artifact does not come from a
     * reactor project.
     * 
     * @TODO should come from separate ReactorArtifactDescriptor
     */
    public ReactorProject getMavenProject();

    /**
     * Maven artifact classifier. Not null only for classified artifacts coming from a reactor
     * project (eg, sources jar).
     * 
     * @TODO should come from separate ReactorArtifactDescriptor
     */
    public String getClassifier();

    /**
     * P2 metadata describing the project
     * 
     * @TODO should come from separate P2ArtifactDescriptor interface
     * @TODO this should probably be Map<String,Set<IInstallableUnit>>
     * @TODO is this dependency-only or final metadata?
     */
    public Collection<IInstallableUnit> getInstallableUnits();

}
