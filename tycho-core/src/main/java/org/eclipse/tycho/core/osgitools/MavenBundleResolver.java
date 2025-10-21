/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.osgitools;

import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.MavenArtifactKey;
import org.eclipse.tycho.ResolvedArtifactKey;

/**
 * Helper interface for resolving bundles that are living in P2 and maven world and
 * where we want to provide the user with the maximum flexibility, mostly in cases where a bundle is
 * required for technical reasons, e.g. some standard OSGi annotations where we know they are
 * available from some maven coordinates Tycho uses that information to automatically fetch the
 * artifact if it is not present in the target.
 */
public interface MavenBundleResolver {

    /**
     * Resolve the specified {@link MavenArtifactKey} from the target platform first and then using
     * Maven if not found there.
     * 
     * @param project
     * @param mavenSession
     * @param mavenArtifactKey
     * @return an optional describing the {@link ResolvedArtifactKey}
     */
    Optional<ResolvedArtifactKey> resolveMavenBundle(MavenProject project, MavenSession mavenSession,
            MavenArtifactKey mavenArtifactKey);

    /**
     * Resolve a Maven artifact using Maven coordinates.
     * 
     * @param project
     * @param mavenSession
     * @param groupId
     * @param artifactId
     * @param version
     * @return an optional describing the {@link ResolvedArtifactKey}
     */
    Optional<ResolvedArtifactKey> resolveMavenBundle(MavenProject project, MavenSession mavenSession,
            String groupId, String artifactId, String version);

}
