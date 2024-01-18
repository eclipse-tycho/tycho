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
package org.eclipse.tycho.apitools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

public class ApiAnalysisResult implements Serializable {

	private List<IApiProblem> problems = new ArrayList<>();
	private List<ResolverError> resolveError = new ArrayList<>();
	private String version;

	public ApiAnalysisResult(String version) {
		this.version = version;
	}

	public Stream<IApiProblem> problems() {
		return problems.stream();
	}

	public Stream<ResolverError> resolveErrors() {
		return resolveError.stream();
	}

	public void addProblem(IApiProblem problem, IProject project) {
		problems.add(new ApiProblemDTO(problem, project));
	}

	public void addResolverError(ResolverError error) {
		resolveError.add(new ResolverErrorDTO(error));
	}

	public String getApiToolsVersion() {
		return version;
	}
}
