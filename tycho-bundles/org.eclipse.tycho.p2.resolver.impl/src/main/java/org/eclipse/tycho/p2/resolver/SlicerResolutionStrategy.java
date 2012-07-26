/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.Collection;
import java.util.LinkedHashSet;
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
import org.eclipse.tycho.core.facade.MavenLogger;

@SuppressWarnings("restriction")
public class SlicerResolutionStrategy extends AbstractSlicerResolutionStrategy {

    public SlicerResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    @Override
    protected Slicer newSlicer(IQueryable<IInstallableUnit> availableIUs, Map<String, String> properties) {
        return new PermissiveSlicer(availableIUs, properties, true, false, ignoreFilters(), strictDependencies(), false);
    }

    protected boolean isSlicerError(MultiStatus slicerStatus) {
        return slicerStatus.matches(IStatus.WARNING | IStatus.ERROR | IStatus.CANCEL);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor) {
        properties = addFeatureJarFilter(properties);

        IQueryable<IInstallableUnit> slice = slice(properties, monitor);

        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>(slice.query(QueryUtil.ALL_UNITS, monitor)
                .toUnmodifiableSet());
        result.removeAll(jreIUs);

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(result, false));
        }

        return result;
    }

    protected boolean ignoreFilters() {
        return true;
    }

    protected boolean strictDependencies() {
        return true;
    }
}
