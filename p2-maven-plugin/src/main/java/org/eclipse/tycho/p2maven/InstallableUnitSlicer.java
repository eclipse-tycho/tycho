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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * A strategy that computes a slice from a set of all units.
 */
@Named
@Singleton
public class InstallableUnitSlicer {

	private static final SlicingOptions DEFAULT_SLICING_OPTIONS = new SlicingOptions();
	@Inject
	private Logger log;

	/**
	 * Computes a "slice" of a given set of {@link IInstallableUnit}s that include
	 * as much of the requirements of the root units that are fulfilled by the
	 * available units. The slice is greedy, that means that as much as possible is
	 * included.
	 * 
	 * @param rootIus     the root units that are inspected for the slice
	 * @param avaiableIUs the {@link IQueryable} of all units that could be used for
	 *                    the slice
	 * @param options     the options used for the slicing
	 * @return the result of the slicing
	 * @throws CoreException if there is any error
	 */
	public IQueryResult<IInstallableUnit> computeDependencies(Collection<IInstallableUnit> rootIus,
			IQueryable<IInstallableUnit> avaiableIUs, SlicingOptions options)
			throws CoreException {
		options = Objects.requireNonNullElse(options, DEFAULT_SLICING_OPTIONS);
		NullProgressMonitor monitor = new NullProgressMonitor();
		PermissiveSlicer slicer = new PermissiveSlicer(avaiableIUs, options.getFilter(),
				options.isIncludeOptionalDependencies(), options.isEverythingGreedy(), options.isForceFilterTo(),
				options.isConsiderStrictDependencyOnly(), options.isFollowOnlyFilteredRequirements());
		IQueryable<IInstallableUnit> slice = slicer.slice(rootIus, monitor);
		IStatus sliceStatus = slicer.getStatus();
		if (sliceStatus.matches(IStatus.ERROR)) {
			throw new CoreException(sliceStatus);
		}
		if (!sliceStatus.isOK()) {
			log.debug("There are warnings from the slicer: " + sliceStatus);
		}
		if (options.isLatestVersionOnly()) {
			return slice.query(QueryUtil.createLatestIUQuery(), monitor);
		}
		return slice.query(QueryUtil.createIUAnyQuery(), monitor);
	}

	/**
	 * Computes a "slice" that is the <b>direct</b> dependencies of the given
	 * {@link IInstallableUnit}s in a way that the result contains any unit that
	 * satisfies a requirement in for rootIus
	 * 
	 * @param rootIus     the root {@link InstallableUnit}s to take into account
	 * @param avaiableIUs the {@link IQueryable} of all units that could be used for
	 *                    fulfilling a requirement
	 * @param contextIUs  context IUs that represent the the profile properties to
	 *                    consider during resolution, can be empty in which case a
	 *                    filter is always considered a match
	 * @return the result of the slicing, be aware that no maximum/minimum
	 *         constraints or filters are applied as part of this computation
	 * @throws CoreException if there is any error
	 */
	public Map<IRequirement, Collection<IInstallableUnit>> computeDirectDependencies(
			Collection<IInstallableUnit> rootIus,
			IQueryable<IInstallableUnit> avaiableIUs) throws CoreException {
		List<IRequirement> collect = rootIus.stream().flatMap(iu -> iu.getRequirements().stream())
				.filter(req -> {
					for (IInstallableUnit unit : rootIus) {
						if (unit.satisfies(req)) {
							// self full filled requirement
							return false;
						}
					}
					return true;
				}).toList();
		Map<IRequirement, Collection<IInstallableUnit>> result = new LinkedHashMap<>(collect.size());
		for (IInstallableUnit iu : avaiableIUs.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet()) {
			for (IRequirement requirement : collect) {
				if (iu.satisfies(requirement)) {
					result.computeIfAbsent(requirement, nil -> new ArrayList<>()).add(iu);
				}
			}
		}
		return result;
	}

}
