/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.util.Hashtable;
import java.util.Map;

public class SlicingOptions {
	private boolean includeOptionalDependencies = true;
	private boolean everythingGreedy = true;
	private boolean forceFilterTo = true;
	private boolean considerStrictDependencyOnly = false;
	private boolean followOnlyFilteredRequirements = false;
	private boolean latestVersion = false;
	private boolean resolve = false;

	private Map<String, String> filter = null;

	public boolean includeOptionalDependencies() {
		return includeOptionalDependencies;
	}

	public void includeOptionalDependencies(boolean optional) {
		this.includeOptionalDependencies = optional;
	}

	public boolean isEverythingGreedy() {
		return everythingGreedy;
	}

	public void everythingGreedy(boolean greedy) {
		this.everythingGreedy = greedy;
	}

	public boolean forceFilterTo() {
		return forceFilterTo;
	}

	public void forceFilterTo(boolean forcedTo) {
		this.forceFilterTo = forcedTo;
	}

	public boolean considerStrictDependencyOnly() {
		return considerStrictDependencyOnly;
	}

	public void considerStrictDependencyOnly(boolean strict) {
		this.considerStrictDependencyOnly = strict;
	}

	public Map<String, String> getFilter() {
		if (filter == null)
			filter = new Hashtable<>();
		return filter;
	}

	public void setFilter(Map<String, String> filter) {
		this.filter = filter;
	}

	public void followOnlyFilteredRequirements(boolean onlyFiltered) {
		this.followOnlyFilteredRequirements = onlyFiltered;
	}

	public boolean followOnlyFilteredRequirements() {
		return followOnlyFilteredRequirements;
	}

	public boolean latestVersionOnly() {
		return latestVersion;
	}

	public void latestVersionOnly(boolean latest) {
		this.latestVersion = latest;
	}

	public void installTimeLikeResolution(boolean resolve) {
		this.resolve = resolve;
	}

	public boolean getInstallTimeLikeResolution() {
		return resolve;
	}
}
