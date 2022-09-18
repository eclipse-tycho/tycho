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

import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.model.Dependency;

/**
 * Plexus components implementing this role are used to transform a file into a
 * "real" maven dependency coordinate.
 *
 */
public interface ArtifactCoordinateResolver {

	/**
	 * Resolves the given path to a maven dependency information
	 * 
	 * @param path the path to query
	 * @return the resolved maven meta-data or {@link Optional#empty()} if nothing
	 *         could be found
	 */
	Optional<Dependency> resolve(Path path);

}
