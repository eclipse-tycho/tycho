/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;
import java.util.Set;

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
     */
    public File getLocation();

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
    public Set<Object/* IInstallableUnit */> getInstallableUnits();
}
