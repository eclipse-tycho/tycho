/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.artifacts.DependencyArtifacts;

/**
 * tycho-specific behaviour associated with MavenProject instances. stateless.
 * 
 * TODO take target environments into account!
 */
public interface TychoProject {
    /**
     * Walks all project dependencies, regardless of runtime environment filters.
     */
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project);

    /**
     * Walks project dependencies resolved for the specified runtime environment.
     */
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project, TargetEnvironment environment);

    /**
     * Returns resolved project dependencies. For projects targeting multiple runtime environments,
     * returned collection includes artifacts for all supported runtime environments.
     */
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project);

    /**
     * Returns resolved project dependencies resolved for specified runtime environment.
     */
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project, TargetEnvironment environment);

    // implementation must not depend on target platform
    // TODO this method is not well-defined for some packaging types; some of them don't produce artifacts addressable via Eclipse coordinates
    public ArtifactKey getArtifactKey(ReactorProject project);

    /**
     * Implicit target environment configuration present in project metadata, like, for example,
     * Eclipse-PlatformFilter OSGi bundle manifest attribute.
     */
    public TargetEnvironment getImplicitTargetEnvironment(MavenProject project);

}
