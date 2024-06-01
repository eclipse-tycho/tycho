/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.core;

import java.util.Collection;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.osgi.framework.Filter;

/**
 * tycho-specific behavior associated with MavenProject instances. stateless.
 * 
 */
public interface TychoProject {
    /**
     * Walks all project dependencies, regardless of runtime environment filters.
     */
    public ArtifactDependencyWalker getDependencyWalker(ReactorProject project);

    /**
     * Returns resolved project dependencies. For projects targeting multiple runtime environments,
     * returned collection includes artifacts for all supported runtime environments.
     */
    public DependencyArtifacts getDependencyArtifacts(ReactorProject project);

    public DependencyArtifacts getDependencyArtifacts(MavenProject project);

    /**
     * Returns resolved project dependencies. For projects targeting multiple runtime environments,
     * returned collection includes artifacts for all supported runtime environments.
     */
    public DependencyArtifacts getTestDependencyArtifacts(ReactorProject project);

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

    public Filter getTargetEnvironmentFilter(MavenProject project);

    /**
     * @return a collection of dependencies (and their transitive dependencies) that where present
     *         before Tycho has injected the target content of the project into the model, also
     *         known as "pom considered dependencies"
     */
    Collection<Artifact> getInitialArtifacts(ReactorProject reactorProject, Collection<String> scopes);

    /**
     * Computes a map for the given artifacts to their facades
     * 
     * @param artifacts
     *            the artifacts to map
     * @return
     */
    Map<Artifact, IArtifactFacade> getArtifactFacades(ReactorProject reactorProject, Collection<Artifact> artifacts);

}
