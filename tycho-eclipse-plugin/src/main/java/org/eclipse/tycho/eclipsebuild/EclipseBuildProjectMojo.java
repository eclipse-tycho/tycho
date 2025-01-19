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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.core.resources.IMarker;

/**
 * This mojo allows to perform an eclipse-build on a project like it would be
 * performed inside the IDE, this can be useful in cases where there are very
 * special builders that are not part of Tycho.
 */
@Mojo(name = "eclipse-build", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class EclipseBuildProjectMojo extends AbstractEclipseBuildMojo<EclipseBuildResult> {

	@Parameter(defaultValue = "true", property = "tycho.eclipsebuild.failOnError")
	private boolean failOnError;

	@Override
	protected String getName() {
		return "Eclipse Project Build";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected EclipseBuild createExecutable() {
		return new EclipseBuild(project.getBasedir().toPath(), debug);
	}

	@Override
	protected void handleResult(EclipseBuildResult result) throws MojoFailureException {
		if (failOnError) {
			List<IMarker> errorMarkers = result.markers()
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR).toList();
			if (errorMarkers.size() > 0) {
				String msg = errorMarkers.stream().map(problem -> asString(problem, result))
						.collect(Collectors.joining(System.lineSeparator()));
				throw new MojoFailureException("There are Build errors:" + System.lineSeparator() + msg);
			}
		}
	}
}
