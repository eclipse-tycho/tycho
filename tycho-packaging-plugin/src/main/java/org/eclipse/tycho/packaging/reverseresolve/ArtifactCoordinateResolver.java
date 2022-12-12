/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.packaging.reverseresolve;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactDescriptor;

/**
 * Plexus components implementing this role are used to transform a file into a
 * "real" maven dependency coordinate.
 *
 */
public interface ArtifactCoordinateResolver {

	/**
	 * Resolves the given raw dependency to a maven artifact GAV
	 * 
	 * @param dependency the raw dependency to resolve
	 * @param project    the project context where the resolve should take place
	 * @param session    the maven session to use
	 * @return the resolved maven meta-data or {@link Optional#empty()} if nothing
	 *         could be found
	 */
	Optional<Dependency> resolve(Dependency dependency, MavenProject project, MavenSession session);

	/**
	 * Get the file System Path for a dependency
	 * 
	 * @param dep
	 * @return the local path or an empty optional if path can not be determined
	 */
	static Optional<Path> getPath(Dependency dep) {
		if (dep instanceof ArtifactDescriptor) {
			ArtifactDescriptor artifactDescriptor = (ArtifactDescriptor) dep;
			File location = artifactDescriptor.getLocation(true);
			if (location != null && location.exists()) {
				return Optional.of(location.toPath().toAbsolutePath());
			}
		} else {
			String systemPath = dep.getSystemPath();
			if (systemPath != null) {
				try {
					Path path = Path.of(systemPath);
					if (Files.exists(path)) {
						return Optional.of(path.toAbsolutePath());
					}
				} catch (InvalidPathException e) {
					// then we can't use it ...
				}
			}
		}
		return Optional.empty();
	}
}
