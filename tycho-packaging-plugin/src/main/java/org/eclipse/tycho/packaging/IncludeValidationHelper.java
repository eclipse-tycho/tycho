/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - also check for files in the classes output directory
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildProperties;

public interface IncludeValidationHelper {

	void checkBinIncludesExist(MavenProject project, BuildProperties buildProperties, boolean strict,
			String... ignoredIncludes) throws MojoExecutionException;

	void checkSourceIncludesExist(MavenProject project, BuildProperties buildProperties, boolean strict)
			throws MojoExecutionException;
}
