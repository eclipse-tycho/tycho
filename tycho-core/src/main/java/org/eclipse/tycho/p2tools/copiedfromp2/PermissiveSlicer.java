/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryable;

public class PermissiveSlicer extends Slicer {
    private boolean includeOptionalDependencies; //Cause optional dependencies not be followed as part of the
    private boolean everythingGreedy;
    private boolean considerFilter;
    private boolean considerOnlyStrictDependency;
    private boolean evalFilterTo;
    private boolean onlyFilteredRequirements;

    public PermissiveSlicer(IQueryable<IInstallableUnit> input, Map<String, String> context,
            boolean includeOptionalDependencies, boolean everythingGreedy, boolean evalFilterTo,
            boolean considerOnlyStrictDependency, boolean onlyFilteredRequirements) {
        super(input, context, true);
        this.considerFilter = context != null && context.size() > 1;
        this.includeOptionalDependencies = includeOptionalDependencies;
        this.everythingGreedy = everythingGreedy;
        this.evalFilterTo = evalFilterTo;
        this.considerOnlyStrictDependency = considerOnlyStrictDependency;
        this.onlyFilteredRequirements = onlyFilteredRequirements;
    }

    @Override
    protected boolean isApplicable(IInstallableUnit iu) {
        if (considerFilter) {
            return super.isApplicable(iu);
        }
        return iu.getFilter() == null || evalFilterTo;
    }

    @Override
    protected boolean isApplicable(IRequirement req) {
        //Every filter in this method needs to continue except when the filter does not pass
        if (!includeOptionalDependencies && req.getMin() == 0) {
            return false;
        }
        if (considerOnlyStrictDependency && !RequiredCapability.isStrictVersionRequirement(req.getMatches())) {
            return false;
        }
        //deal with filters
        IMatchExpression<IInstallableUnit> filter = req.getFilter();
        if (filter == null) {
            return !onlyFilteredRequirements;
        }
        return considerFilter ? filter.isMatch(selectionContext) : evalFilterTo;
    }

    @Override
    protected boolean isGreedy(IRequirement req) {
        return everythingGreedy || super.isGreedy(req);
    }
}
