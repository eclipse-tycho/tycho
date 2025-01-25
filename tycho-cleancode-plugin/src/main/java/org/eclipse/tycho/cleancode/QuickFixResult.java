/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.cleancode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tycho.eclipsebuild.EclipseBuildResult;

public class QuickFixResult extends EclipseBuildResult {

	private Set<String> tried = new HashSet<String>();
	private List<String> fixed = new ArrayList<>();
	private int markers;

	public Stream<String> fixes() {
		return fixed.stream();
	}

	public void addFix(String fix) {
		fixed.add(fix);
	}

	public boolean isEmpty() {
		return fixed.isEmpty();
	}

	public boolean tryFix(IMarker marker) {
		String msg = marker.getAttribute(IMarker.MESSAGE, "");
		String resource = marker.getResource().toString();
		int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
		String type;
		try {
			type = marker.getType();
		} catch (CoreException e) {
			type = "";
		}
		String key = type + " " + resource + ":" + line + " " + msg;
		return tried.add(key);
	}

}
