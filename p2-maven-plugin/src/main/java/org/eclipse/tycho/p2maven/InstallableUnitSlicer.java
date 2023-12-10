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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * A strategy that computes a slice from a set of all units.
 */
@Component(role = InstallableUnitSlicer.class)
public class InstallableUnitSlicer {

	@Requirement
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
	 * @return the result of the slicing
	 * @throws CoreException if there is any error
	 */
	public IQueryResult<IInstallableUnit> computeDependencies(Collection<IInstallableUnit> rootIus,
			IQueryable<IInstallableUnit> avaiableIUs) throws CoreException {
		NullProgressMonitor monitor = new NullProgressMonitor();
		PermissiveSlicer slicer = new TychoSlicer(avaiableIUs);
		IQueryable<IInstallableUnit> slice = slicer.slice(rootIus, monitor);
		IStatus sliceStatus = slicer.getStatus();
		if (sliceStatus.matches(IStatus.ERROR)) {
			throw new CoreException(sliceStatus);
		}
		if (!sliceStatus.isOK()) {
			log.debug("There are warnings from the slicer: " + sliceStatus);
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
	 * @return the result of the slicing
	 * @throws CoreException if there is any error
	 */
	public IQueryResult<IInstallableUnit> computeDirectDependencies(Collection<IInstallableUnit> rootIus,
			IQueryable<IInstallableUnit> avaiableIUs, Collection<IInstallableUnit> contextIUs) throws CoreException {
		Collection<IInstallableUnit> result = new LinkedHashSet<>();
		Map<Boolean, List<IRequirement>> collect = rootIus.stream().flatMap(iu -> iu.getRequirements().stream())
				.filter(req -> {
					for (IInstallableUnit unit : rootIus) {
						if (unit.satisfies(req)) {
							// self full filled requirement
							return false;
						}
					}
					return isMatch(req.getFilter(), contextIUs);
				}).collect(Collectors.partitioningBy(req -> req.getMax() == 0));
		List<IRequirement> negativeRequirements = collect.get(true);
		List<IRequirement> requirements = new ArrayList<>(collect.get(false));

		for (IInstallableUnit iu : avaiableIUs.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet()) {
			for (Iterator<IRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
				IRequirement requirement = iterator.next();
				if (iu.satisfies(requirement)) {
					result.add(iu);
					if (requirement.getMax() == 1) {
						// only one provider allowed
						iterator.remove();
					}
					break;
				}
			}
			// now check if the IU satisfies any negative one, then we need to remove it
			// from the set...
			for (IRequirement requirement : negativeRequirements) {
				if (iu.satisfies(requirement)) {
					result.remove(iu);
					break;
				}
			}
		}
		return new CollectionResult<>(result);
	}

	private boolean isMatch(IMatchExpression<IInstallableUnit> filter, Collection<IInstallableUnit> contextIUs) {
		if (filter == null || contextIUs.isEmpty()) {
			return true;
		}
		return contextIUs.stream().anyMatch(contextIU -> filter.isMatch(contextIU));
	}

	private final class TychoSlicer extends PermissiveSlicer {
		private TychoSlicer(IQueryable<IInstallableUnit> input) {
			super(input, new HashMap<>(), //
					true, // includeOptionalDependencies
					true, // everythingGreedy
					true, // evalFilterTo
					false, // considerOnlyStrictDependency
					false // onlyFilteredRequirements
			);
		}

		@Override
		protected boolean isApplicable(IInstallableUnit unit, IRequirement requirement) {
			if (requirement.isMatch(unit)) {
				// a bundle might import its exported packages, in such a case we ignore the
				// requirement
				log.debug("The requirement " + requirement + " is already satisfied by the unit " + unit
						+ " itself, ignoring it.");
				return false;
			}
			return super.isApplicable(unit, requirement);
		}
	}

}
