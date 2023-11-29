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

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

public class ApiProblemDTO implements IApiProblem, Serializable {

	private final int severity;
	private final int elementKind;
	private final int messageid;
	private final String resourcePath;
	private final String typeName;
	private final String[] messageArguments;
	private final int charStart;
	private final int charEnd;
	private final int lineNumber;
	private final int category;
	private final int id;
	private final String message;
	private final int kind;
	private final int flags;
	private final String toString;

	public ApiProblemDTO(IApiProblem problem, IProject project) {
		severity = ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(problem), project);
		elementKind = problem.getElementKind();
		messageid = problem.getMessageid();
		resourcePath = problem.getResourcePath();
		typeName = problem.getTypeName();
		messageArguments = problem.getMessageArguments();
		charStart = problem.getCharStart();
		charEnd = problem.getCharEnd();
		lineNumber = problem.getLineNumber();
		category = problem.getCategory();
		id = problem.getId();
		message = problem.getMessage();
		kind = problem.getKind();
		flags = problem.getFlags();
		toString = problem.toString();
	}

	@Override
	public int getSeverity() {
		return severity;
	}

	@Override
	public int getElementKind() {
		return elementKind;
	}

	@Override
	public int getMessageid() {
		return messageid;
	}

	@Override
	public String getResourcePath() {
		return resourcePath;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public String[] getMessageArguments() {
		return messageArguments;
	}

	@Override
	public int getCharStart() {
		return charStart;
	}

	@Override
	public int getCharEnd() {
		return charEnd;
	}

	@Override
	public int getLineNumber() {
		return lineNumber;
	}

	@Override
	public int getCategory() {
		return category;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public int getKind() {
		return kind;
	}

	@Override
	public int getFlags() {
		return flags;
	}

	@Override
	public String[] getExtraMarkerAttributeIds() {
		return new String[0];
	}

	@Override
	public Object[] getExtraMarkerAttributeValues() {
		return new Object[0];
	}

	@Override
	public String toString() {
		return toString;
	}

}
