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
package org.eclipse.tycho.p2.impl.resolver;

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
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.facade.MavenLogger;

@SuppressWarnings("restriction")
public class SlicerResolutionStrategy extends ResolutionStrategy {
    private static final IInstallableUnit[] EMPTY_IU_ARRAY = new IInstallableUnit[0];

    public SlicerResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor) {
        properties = addFeatureJarFilter(properties);

        @SuppressWarnings("unchecked")
        IQueryable<IInstallableUnit> availableIUs = new CompoundQueryable<IInstallableUnit>(toArray(this.availableIUs,
                new CollectionResult<IInstallableUnit>(jreIUs)));

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Available IUs:\n" + ResolverDebugUtils.toDebugString(availableIUs, false, monitor));
            logger.debug("Root IUs:\n" + ResolverDebugUtils.toDebugString(rootIUs, true));
            logger.debug("Extra IUs:\n" + ResolverDebugUtils.toDebugString(rootIUs, true));
        }

        Slicer slicer = new PermissiveSlicer(availableIUs, properties, true, false, ignoreFilters(),
                strictDependencies(), false);

        Set<IInstallableUnit> seedUnits = new LinkedHashSet<IInstallableUnit>(this.rootIUs);
        seedUnits.addAll(createAdditionalRequirementsIU());

        IQueryable<IInstallableUnit> slice = slicer.slice(seedUnits.toArray(EMPTY_IU_ARRAY), monitor);

        MultiStatus slicerStatus = slicer.getStatus();
        if (slice == null || slicerStatus.matches(IStatus.WARNING | IStatus.ERROR | IStatus.CANCEL)) {
            throw newResolutionException(slicer.getStatus());
        }

        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>(slice.query(QueryUtil.ALL_UNITS, monitor)
                .toUnmodifiableSet());
        result.removeAll(jreIUs);

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(result, false));
        }

        return result;
    }

    private static <T> T[] toArray(T... t) {
        return t;
    }

    protected boolean ignoreFilters() {
        return true;
    }

    protected boolean strictDependencies() {
        return true;
    }
}
