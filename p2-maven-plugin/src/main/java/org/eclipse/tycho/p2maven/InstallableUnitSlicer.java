/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.Collection;
import java.util.HashMap;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
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
	 * @throws CoreException if there is any
	 */
	public IQueryResult<IInstallableUnit> resolve(Collection<IInstallableUnit> rootIus,
			IQueryable<IInstallableUnit> avaiableIUs)
			throws CoreException {
		boolean includeOptionalDependencies = true;
		boolean everythingGreedy = true;
		boolean evalFilterTo = true;
		boolean strictDependency = false;
		boolean onlyFilteredRequirements = false;
		NullProgressMonitor monitor = new NullProgressMonitor();
		PermissiveSlicer slicer = new PermissiveSlicer(avaiableIUs, new HashMap<String, String>(), includeOptionalDependencies, everythingGreedy, evalFilterTo,
				strictDependency, onlyFilteredRequirements);
		IQueryable<IInstallableUnit> slice = slicer.slice(rootIus.toArray(IInstallableUnit[]::new),
				monitor);
		IStatus sliceStatus = slicer.getStatus();
		if (sliceStatus.matches(IStatus.ERROR)) {
			throw new CoreException(sliceStatus);
		}
		if (!sliceStatus.isOK()) {
			log.debug("There are warnings from the slicer: " + sliceStatus);
		}
		return slice.query(QueryUtil.createIUAnyQuery(), monitor);
	}

}
