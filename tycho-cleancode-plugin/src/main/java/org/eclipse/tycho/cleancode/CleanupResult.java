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

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.tycho.eclipsebuild.EclipseBuildResult;

public class CleanupResult extends EclipseBuildResult {

	private Set<String> cleanups = new TreeSet<String>();

	public void addCleanup(String cleanup) {
		cleanups.add(cleanup);
	}

	public Stream<String> cleanups() {
		return this.cleanups.stream();
	}

}
