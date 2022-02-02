/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.resolution;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;

@SuppressWarnings("restriction")
public class SlicerResolutionStrategy extends AbstractSlicerResolutionStrategy {

    private boolean ignoreFilters;

    /**
     * @param ignoreFilters
     *            treat all filters as if they weren't present. Equivalent to evaluating all filters
     *            to true.
     */
    public SlicerResolutionStrategy(MavenLogger logger, boolean ignoreFilters) {
        super(logger);
        this.ignoreFilters = ignoreFilters;
    }

    @Override
    protected Slicer newSlicer(IQueryable<IInstallableUnit> availableIUs, Map<String, String> properties) {
        final Map<String, String> context;
        final boolean evalFilterTo;
        if (ignoreFilters) {
            context = Collections.emptyMap(); // to get PermissiveSlicer.considerFilter = false
            evalFilterTo = true;
        } else {
            context = properties;

            // this is needed so that the PermissiveSlicer evaluates filters
            // TODO fix tests that fail if the following is activated
//            if (properties.size() <= 1)
//                throw new IllegalStateException();
            evalFilterTo = false; // ... and then this value doesn't matter
        }
        return new PermissiveSlicer(availableIUs, context, true, false, evalFilterTo, true, false);
    }

    @Override
    protected boolean isSlicerError(MultiStatus slicerStatus) {
        return slicerStatus.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    @Override
    public Collection<IInstallableUnit> multiPlatformResolve(List<TargetEnvironment> environments,
            IProgressMonitor monitor) throws ResolverException {
        if (ignoreFilters) {
            // short cut: properties would ignored for each single resolution, so resolve just once 
            return resolve(Collections.<String, String> emptyMap(), monitor);
        }
        return super.multiPlatformResolve(environments, monitor);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {

        IQueryable<IInstallableUnit> slice = slice(properties, monitor);

        Set<IInstallableUnit> result = new LinkedHashSet<>(slice.query(QueryUtil.ALL_UNITS, monitor)
                .toUnmodifiableSet());
        result.removeAll(data.getEEResolutionHints().getTemporaryAdditions());

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(result, false));
        }

        return result;
    }

}
