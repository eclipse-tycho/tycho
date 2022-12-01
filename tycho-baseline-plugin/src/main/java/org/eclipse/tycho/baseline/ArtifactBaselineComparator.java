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

import java.io.InputStream;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.ReactorProject;

public interface ArtifactBaselineComparator {
	/**
	 * Selects from a result the artifact that could be compared by this comparator
	 * 
	 * @param result the quer
	 * @return
	 */
	IInstallableUnit selectIU(IQueryable<IInstallableUnit> result);

	void compare(ReactorProject project, Supplier<InputStream> baselineArtifact, BaselineContext context)
			throws Exception;

}
