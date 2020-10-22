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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;
import org.eclipse.tycho.p2.impl.publisher.FeatureDependenciesAction;
import org.eclipse.tycho.repository.p2base.metadata.QueryableCollection;

@SuppressWarnings("restriction")
public class DependencyCollector extends AbstractResolutionStrategy {

    public DependencyCollector(MavenLogger logger) {
        super(logger);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor) {
        Set<IInstallableUnit> result = new LinkedHashSet<>();

        LinkedHashSet<IStatus> errors = new LinkedHashSet<>();

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Available IUs:\n" + ResolverDebugUtils.toDebugString(data.getAvailableIUs(), false));
            logger.debug("Root IUs:\n" + ResolverDebugUtils.toDebugString(data.getRootIUs(), true));
        }

        result.addAll(data.getRootIUs());

        QueryableCollection availableUIsQueryable = new QueryableCollection(data.getAvailableIUs());
        for (IInstallableUnit iu : data.getRootIUs()) {
            collectIncludedIUs(availableUIsQueryable, result, errors, iu, true, monitor);
        }

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Collected IUs:\n" + ResolverDebugUtils.toDebugString(result, false));
        }

        // TODO additionalRequirements

        if (!errors.isEmpty()) {
            MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, 0, errors.toArray(new IStatus[errors.size()]),
                    "Missing dependencies", null);

            throw new RuntimeException(status.toString(), new ProvisionException(status));
        }

        return result;
    }

    private void collectIncludedIUs(IQueryable<IInstallableUnit> availableIUs, Set<IInstallableUnit> result,
            Set<IStatus> errors, IInstallableUnit iu, boolean immediate, IProgressMonitor monitor) {
        // features listed in site.xml directly
        // features/bundles included in included features (RequiredCapability.isVersionStrict is approximation of this)

        for (IRequirement req : iu.getRequirements()) {
            IQueryResult<IInstallableUnit> matches = availableIUs
                    .query(QueryUtil.createLatestQuery(QueryUtil.createMatchQuery(req.getMatches())), monitor);

            if (!matches.isEmpty()) {
                IInstallableUnit match = matches.iterator().next(); // can only be one

                if (immediate || isIncluded(iu, req, match)) {
                    result.add(match);

                    if (isFeature(match)) {
                        collectIncludedIUs(availableIUs, result, errors, match, false, monitor);
                    }
                }
            } else {
                errors.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Unable to find dependency from " + iu.toString() + " to " + req.toString()));
            }
        }
    }

    private boolean isIncluded(IInstallableUnit iu, IRequirement req, IInstallableUnit match) {
        Set<String> includedIUs = FeatureDependenciesAction.getIncludedUIs(iu);

        if (includedIUs.contains(match.getId())) {
            return true;
        }

        return RequiredCapability.isStrictVersionRequirement(req.getMatches());
    }

    private boolean isFeature(IInstallableUnit iu) {
        return QueryUtil.isGroup(iu);
    }
}
