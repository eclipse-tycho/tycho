/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2resolver;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.resolver.target.P2TargetPlatform;

final class ShadowedUnitsQueryable implements IQueryable<IInstallableUnit> {
    private final P2TargetPlatform targetPlatform;
    private final IQueryable<IInstallableUnit> availableUnits;
    private Set<IInstallableUnit> usedShadowedUnits;

    ShadowedUnitsQueryable(P2TargetPlatform targetPlatform, IQueryable<IInstallableUnit> availableUnits,
            Set<IInstallableUnit> usedShadowedUnits) {
        this.targetPlatform = targetPlatform;
        this.availableUnits = availableUnits;
        this.usedShadowedUnits = usedShadowedUnits;
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        IQueryResult<IInstallableUnit> result = availableUnits.query(query, monitor);
        if (result.isEmpty()) {
            if (targetPlatform instanceof TargetPlatformBaseImpl) {
                TargetPlatformBaseImpl preliminaryTargetPlatform = (TargetPlatformBaseImpl) targetPlatform;
                Set<IInstallableUnit> shadowed = preliminaryTargetPlatform.getShadowed();
                IQueryResult<IInstallableUnit> shadowedResult = query.perform(shadowed.iterator());
                if (!shadowedResult.isEmpty()) {
                    for (IInstallableUnit unit : shadowedResult) {
                        usedShadowedUnits.add(unit);
                    }
                    return shadowedResult;
                }
            }
        }
        return result;
    }
}
