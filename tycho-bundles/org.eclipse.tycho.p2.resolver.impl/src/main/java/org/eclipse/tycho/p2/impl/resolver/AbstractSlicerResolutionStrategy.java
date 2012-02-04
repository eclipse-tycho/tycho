/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.facade.MavenLogger;

@SuppressWarnings("restriction")
public abstract class AbstractSlicerResolutionStrategy extends AbstractResolutionStrategy {

    protected AbstractSlicerResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    protected IQueryable<IInstallableUnit> slice(Map<String, String> properties, IProgressMonitor monitor) {

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Properties: " + properties.toString());
            logger.debug("Available IUs:\n" + ResolverDebugUtils.toDebugString(availableIUs, false));
            logger.debug("JRE IUs:\n" + ResolverDebugUtils.toDebugString(jreIUs, false));
            logger.debug("Root IUs:\n" + ResolverDebugUtils.toDebugString(rootIUs, true));

            if (additionalRequirements != null && !additionalRequirements.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (IRequirement req : additionalRequirements) {
                    sb.append("   ").append(req.toString()).append("\n");
                }
                logger.debug("Extra Requirements:\n" + sb.toString());
            }
        }

        Set<IInstallableUnit> availableIUs = new LinkedHashSet<IInstallableUnit>(this.availableIUs);
        availableIUs.addAll(this.jreIUs);

        Set<IInstallableUnit> seedIUs = new LinkedHashSet<IInstallableUnit>(this.rootIUs);
        if (additionalRequirements != null && !additionalRequirements.isEmpty()) {
            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            String time = Long.toString(System.currentTimeMillis());
            iud.setId("tycho-extra-" + time);
            iud.setVersion(Version.createOSGi(0, 0, 0, time));
            iud.setRequirements(additionalRequirements.toArray(new IRequiredCapability[additionalRequirements.size()]));
            seedIUs.add(MetadataFactory.createInstallableUnit(iud));
        }
        seedIUs.addAll(this.jreIUs);

        Slicer slicer = newSlicer(new QueryableCollection(availableIUs), properties);
        IQueryable<IInstallableUnit> slice = slicer.slice(seedIUs.toArray(EMPTY_IU_ARRAY), monitor);
        MultiStatus slicerStatus = slicer.getStatus();
        if (slice == null || isSlicerError(slicerStatus)) {
            throw newResolutionException(slicer.getStatus());
        }

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Slice:\n" + ResolverDebugUtils.toDebugString(slice, false, monitor));
        }

        return slice;
    }

    protected abstract boolean isSlicerError(MultiStatus slicerStatus);

    protected abstract Slicer newSlicer(IQueryable<IInstallableUnit> availableIUs, Map<String, String> properties);
}
