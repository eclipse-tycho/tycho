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
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.facade.TargetEnvironment;
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
     * Returns resolved project dependencies. For projects targeting multiple runtime environments,
     * returned collection includes artifacts for all supported runtime environments.
     */
    public DependencyArtifacts getDependencyArtifacts(MavenProject project);

    /**
     * Returns resolved project dependencies resolved for specified runtime environment.
     */
    public DependencyArtifacts getDependencyArtifacts(MavenProject project, TargetEnvironment environment);

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

    /**
     * Project target platform, i.e. set of artifacts and their corresponding metadata used during
     * project dependency resolution.
     */
    public TargetPlatform getTargetPlatform(MavenProject project);
}
