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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import org.apache.maven.project.MavenProject;

public interface ArtifactBaselineComparator {

	/**
	 * 
	 * @param project
	 * @param context
	 * @return <code>true</code> if the project was compared, or <code>false</code>
	 *         if no baseline artifact could be compared because it was not found.
	 * @throws Exception
	 */
	boolean compare(MavenProject project, BaselineContext context) throws Exception;

}
