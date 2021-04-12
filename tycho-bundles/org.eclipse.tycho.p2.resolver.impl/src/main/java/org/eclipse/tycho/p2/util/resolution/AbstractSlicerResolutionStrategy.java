/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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

import static org.eclipse.tycho.p2.util.resolution.ResolverDebugUtils.toDebugString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.repository.p2base.metadata.QueryableCollection;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
abstract class AbstractSlicerResolutionStrategy extends AbstractResolutionStrategy {

    protected AbstractSlicerResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    protected final IQueryable<IInstallableUnit> slice(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Properties: " + properties.toString());
            logger.debug("Available IUs:\n" + toDebugString(data.getAvailableIUs(), false));
            logger.debug("JRE IUs:\n" + toDebugString(data.getEEResolutionHints().getMandatoryUnits(), false));
            logger.debug("Root IUs:\n" + toDebugString(data.getRootIUs(), true));

            if (data.getAdditionalRequirements() != null && !data.getAdditionalRequirements().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (IRequirement req : data.getAdditionalRequirements()) {
                    sb.append("   ").append(req.toString()).append("\n");
                }
                logger.debug("Extra Requirements:\n" + sb.toString());
            }
        }

        Set<IInstallableUnit> availableIUs = new LinkedHashSet<>(data.getAvailableIUs());
        availableIUs.addAll(data.getEEResolutionHints().getTemporaryAdditions());
        availableIUs.addAll(data.getEEResolutionHints().getMandatoryUnits());

        Set<IInstallableUnit> seedIUs = new LinkedHashSet<>(data.getRootIUs());
        if (data.getAdditionalRequirements() != null && !data.getAdditionalRequirements().isEmpty()) {
            seedIUs.add(createUnitRequiring("tycho-extra", null, data.getAdditionalRequirements()));
        }

        // make sure profile UIs are part of the slice
        seedIUs.addAll(data.getEEResolutionHints().getMandatoryUnits());
        if (!data.getEEResolutionHints().getMandatoryRequires().isEmpty()) {
            seedIUs.add(createUnitRequiring("tycho-ee", null, data.getEEResolutionHints().getMandatoryRequires()));
        }

        Slicer slicer = newSlicer(new QueryableCollection(availableIUs), properties);
        IQueryable<IInstallableUnit> slice = slicer.slice(seedIUs.toArray(EMPTY_IU_ARRAY), monitor);
        MultiStatus slicerStatus = slicer.getStatus();
        if (slice == null || isSlicerError(slicerStatus)) {
            throw new ResolverException(StatusTool.toLogMessage(slicerStatus), properties.toString(),
                    StatusTool.findException(slicerStatus));
        }

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Slice:\n" + ResolverDebugUtils.toDebugString(slice, false, monitor));
        }

        return slice;
    }

    protected abstract boolean isSlicerError(MultiStatus slicerStatus);

    protected abstract Slicer newSlicer(IQueryable<IInstallableUnit> availableIUs, Map<String, String> properties);

    protected static IInstallableUnit createUnitRequiring(String name, Collection<IInstallableUnit> units,
            Collection<IRequirement> additionalRequirements) {

        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString(System.currentTimeMillis());
        result.setId(name + "-" + time);
        result.setVersion(Version.createOSGi(0, 0, 0, time));

        ArrayList<IRequirement> requirements = new ArrayList<>();
        if (units != null) {
            for (IInstallableUnit unit : units) {
                requirements.add(createStrictRequirementTo(unit));
            }
        }
        if (additionalRequirements != null) {
            requirements.addAll(additionalRequirements);
        }

        result.addRequirements(requirements);
        return MetadataFactory.createInstallableUnit(result);
    }

    private static IRequirement createStrictRequirementTo(IInstallableUnit unit) {
        VersionRange strictRange = new VersionRange(unit.getVersion(), true, unit.getVersion(), true);
        int min = 1;
        int max = Integer.MAX_VALUE;
        boolean greedy = true;
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(),
                strictRange, unit.getFilter(), min, max, greedy);
        return requirement;
    }
}
