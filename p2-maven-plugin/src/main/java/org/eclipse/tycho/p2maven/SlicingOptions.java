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
package org.eclipse.tycho.p2maven;

import java.util.Hashtable;
import java.util.Map;

/**
 * Holds the slicing options for a permissive slicer, the defaults are:
 * <ul>
 * <li>includeOptionalDependencies = true</li>
 * <li>everythingGreedy = true</li>
 * <li>forceFilterTo = true</li>
 * <li>considerStrictDependencyOnly = false</li>
 * <li>followOnlyFilteredRequirements = false</li>
 * <li>filter = null</li>
 * <li>latestVersion = true</li>
 * </ul>
 * This effectively means that everything that can be included will be included
 * in the slice.
 */
public class SlicingOptions {
	private boolean includeOptionalDependencies = true;
	private boolean everythingGreedy = true;
	private boolean forceFilterTo = true;
	private boolean considerStrictDependencyOnly = false;
	private boolean followOnlyFilteredRequirements = false;
	private boolean latestVersion = true;
	
	private Map<String, String> filter = null;

	public boolean isIncludeOptionalDependencies() {
		return includeOptionalDependencies;
	}

	public void setIncludeOptionalDependencies(boolean optional) {
		this.includeOptionalDependencies = optional;
	}

	public boolean isEverythingGreedy() {
		return everythingGreedy;
	}

	public void setEverythingGreedy(boolean greedy) {
		this.everythingGreedy = greedy;
	}

	public boolean isForceFilterTo() {
		return forceFilterTo;
	}

	public void setForceFilterTo(boolean forcedTo) {
		this.forceFilterTo = forcedTo;
	}

	public boolean isConsiderStrictDependencyOnly() {
		return considerStrictDependencyOnly;
	}

	public void setConsiderStrictDependencyOnly(boolean strict) {
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

	public void setFollowOnlyFilteredRequirements(boolean onlyFiltered) {
		this.followOnlyFilteredRequirements = onlyFiltered;
	}

	public boolean isFollowOnlyFilteredRequirements() {
		return followOnlyFilteredRequirements;
	}

	public boolean isLatestVersionOnly() {
		return latestVersion;
	}

	public void setLatestVersionOnly(boolean latest) {
		this.latestVersion = latest;
	}
}
