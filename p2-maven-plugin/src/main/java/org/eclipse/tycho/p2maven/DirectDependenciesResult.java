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
package org.eclipse.tycho.p2maven;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class DirectDependenciesResult {

	private Map<IRequirement, List<IInstallableUnit>> unitMap;

	DirectDependenciesResult(Map<IRequirement, List<IInstallableUnit>> unitMap) {
		this.unitMap = unitMap;
	}

	/**
	 * @return a all units contained in the results
	 */
	public List<IInstallableUnit> units() {
		return unitMap.values().stream().flatMap(Collection::stream).distinct().toList();
	}

	public Collection<IRequirement> requirements() {
		return unitMap.keySet();
	}

	public Collection<IInstallableUnit> getUnits(IRequirement requirement) {
		return unitMap.getOrDefault(requirement, List.of());
	}
}
