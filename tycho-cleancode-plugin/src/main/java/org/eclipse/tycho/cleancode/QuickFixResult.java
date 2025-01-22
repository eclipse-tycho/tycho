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
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.tycho.eclipsebuild.EclipseBuildResult;

public class QuickFixResult extends EclipseBuildResult {

	private List<String> fixed = new ArrayList<>();
	private int markers;

	public Stream<String> fixes() {
		return fixed.stream();
	}

	public void addFix(String fix) {
		fixed.add(fix);
	}

	public void setNumberOfMarker(int markers) {
		this.markers = markers;
	}

	public int getMarkers() {
		return markers;
	}

	public boolean isEmpty() {
		return fixed.isEmpty();
	}

}
