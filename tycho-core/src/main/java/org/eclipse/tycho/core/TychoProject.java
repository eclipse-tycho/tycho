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
package org.eclipse.tycho.core;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;

/**
 * tycho-specific behaviour associated with MavenProject instances. stateless.
 * 
 * TODO take target environments into account!
 */
public interface TychoProject {
    /**
     * Walks all project dependencies, regardless of runtime environment filters.
     */
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project);

    /**
     * Walks project dependencies resolved for the specified runtime environment.
     */
    public ArtifactDependencyWalker getDependencyWalker(MavenProject project, TargetEnvironment environment);

    /**
     * Returns project build target platform. For projects targeting multiple runtime environments,
     * returned target platforms includes artifacts for all supported runtime environments.
     */
    public TargetPlatform getTargetPlatform(MavenProject project);

    /**
     * Returns project build target platform resolved for specified runtime environment.
     */
    public TargetPlatform getTargetPlatform(MavenProject project, TargetEnvironment environment);

    // implementation must not depend on target platform
    public ArtifactKey getArtifactKey(ReactorProject project);

    /**
     * Implicit target environment configuration present in project metadata, like, for example,
     * Eclipse-PlatformFilter OSGi bundle manifest attribute.
     */
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project);

    /**
     * Project target execution environment used during the build or null.
     */
    public ExecutionEnvironment getExecutionEnvironment(MavenProject project);
}
