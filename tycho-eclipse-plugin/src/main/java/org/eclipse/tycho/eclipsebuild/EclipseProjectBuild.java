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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import java.nio.file.Path;

import org.eclipse.core.resources.IProject;

public class EclipseProjectBuild extends AbstractEclipseBuild<EclipseBuildResult> {

	EclipseProjectBuild(Path projectDir, boolean debug) {
		super(projectDir, debug);
	}

	@Override
	protected EclipseBuildResult createResult(IProject project) {
		return new EclipseBuildResult();
	}

}
