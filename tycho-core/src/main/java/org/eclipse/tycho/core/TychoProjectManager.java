/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.model.project.EclipseProject;

import aQute.bnd.osgi.Processor;

/**
 * Split into interface it participating in object graph circle (Sisu/Guice can create proxy only out of iface).
 */
public interface TychoProjectManager {

    String CTX_TARGET_PLATFORM_CONFIGURATION = "TychoProjectManager/targetPlatformConfiguration";

    ExecutionEnvironmentConfiguration getExecutionEnvironmentConfiguration(MavenProject project);

    void readExecutionEnvironmentConfiguration(ReactorProject project, ExecutionEnvironmentConfiguration sink);

    Collection<IInstallableUnit> getContextIUs(MavenProject project);

    Map<String, String> getProfileProperties(MavenProject project, TargetEnvironment environment);

    TargetPlatformConfiguration getTargetPlatformConfiguration(MavenProject project);

    TargetPlatformConfiguration getTargetPlatformConfiguration(ReactorProject project);

    Collection<TargetEnvironment> getTargetEnvironments(MavenProject project);

    Optional<TychoProject> getTychoProject(MavenProject project);

    Optional<DependencyArtifacts> getDependencyArtifacts(MavenProject project);

    Optional<TychoProject> getTychoProject(ReactorProject project);

    Optional<ArtifactKey> getArtifactKey(MavenProject project);

    Optional<ArtifactKey> getArtifactKey(ReactorProject project);

    ArtifactKey getArtifactKey(Artifact artifact);

    Optional<EclipseProject> getEclipseProject(MavenProject project);

    Optional<EclipseProject> getEclipseProject(File baseDir);

    Optional<Processor> getBndTychoProject(MavenProject project);

    Collection<Path> getProjectDependencies(MavenProject project) throws Exception;

    Optional<TargetPlatform> getTargetPlatform(MavenProject project);
}
