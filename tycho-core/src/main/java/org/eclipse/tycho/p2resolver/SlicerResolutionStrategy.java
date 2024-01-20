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
package org.eclipse.tycho.p2resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2tools.copiedfromp2.PermissiveSlicer;
import org.eclipse.tycho.p2tools.copiedfromp2.Slicer;

public class SlicerResolutionStrategy extends AbstractSlicerResolutionStrategy {

    private boolean ignoreFilters;
    private boolean warnIfMissing;

    /**
     * @param ignoreFilters
     *            treat all filters as if they weren't present. Equivalent to evaluating all filters
     *            to true.
     */
    public SlicerResolutionStrategy(MavenLogger logger, boolean ignoreFilters, boolean warnIfMissing) {
        super(logger);
        this.ignoreFilters = ignoreFilters;
        this.warnIfMissing = warnIfMissing;
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
        if (warnIfMissing && logger.isExtendedDebugEnabled()) {
            var msg = new StringBuilder(
                    "Following dependencies were not found by the slicer (you can disregard this if it is intentional):\n");
            var anyWarnPresent = false;
            for (var statusItem : slicerStatus.getChildren()) {
                if (statusItem.getSeverity() == IStatus.WARNING) {
                    anyWarnPresent = true;
                    msg.append(statusItem.getMessage()).append("\n");
                }
            }
            if (anyWarnPresent) {
                logger.warn(msg.toString());
            }
        }
        return slicerStatus.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    @Override
    public Collection<IInstallableUnit> multiPlatformResolve(List<TargetEnvironment> environments,
            IProgressMonitor monitor) throws ResolverException {
        if (ignoreFilters) {
            // short cut: properties would ignored for each single resolution, so resolve just once
            return resolve(Collections.emptyMap(), monitor);
        }
        return super.multiPlatformResolve(environments, monitor);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {

        IQueryable<IInstallableUnit> slice = slice(properties, monitor);

        Set<IInstallableUnit> result = new LinkedHashSet<>(
                slice.query(QueryUtil.ALL_UNITS, monitor).toUnmodifiableSet());
        result.removeAll(data.getEEResolutionHints().getTemporaryAdditions());

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(result, false));
        }

        return result;
    }

}
