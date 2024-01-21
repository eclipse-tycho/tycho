/*******************************************************************************
 * Copyright (c) 2013, 2018 Rapicorp Inc. and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Rapicorp, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2tools.copiedfromp2.Projector.AbstractVariable;
import org.sat4j.pb.tools.WeightedObject;

public class OptimizationFunction {

    private IQueryable<IInstallableUnit> picker;
    private IInstallableUnit selectionContext;
    protected Map<String, Map<Version, IInstallableUnit>> slice; //The IUs that have been considered to be part of the problem
    private IQueryable<IInstallableUnit> lastState;
    private List<AbstractVariable> optionalRequirementVariable;

    public OptimizationFunction(IQueryable<IInstallableUnit> lastState, List<AbstractVariable> abstractVariables,
            List<AbstractVariable> optionalRequirementVariable, IQueryable<IInstallableUnit> picker,
            IInstallableUnit selectionContext, Map<String, Map<Version, IInstallableUnit>> slice) {
        this.lastState = lastState;
        this.optionalRequirementVariable = optionalRequirementVariable;
        this.picker = picker;
        this.selectionContext = selectionContext;
        this.slice = slice;
    }

    //Create an optimization function favoring the highest version of each IU
    public List<WeightedObject<? extends Object>> createOptimizationFunction(IInstallableUnit metaIu,
            Collection<IInstallableUnit> newRoots) {
        int numberOfInstalledIUs = sizeOf(lastState);
        List<WeightedObject<? extends Object>> weightedObjects = new ArrayList<>();

        Set<IInstallableUnit> transitiveClosure; //The transitive closure of the IUs we are adding (this also means updating)
        if (newRoots.isEmpty()) {
            transitiveClosure = Collections.emptySet();
        } else {
            IQueryable<IInstallableUnit> queryable = new Slicer(picker, selectionContext, false).slice(newRoots, null);
            if (queryable == null) {
                transitiveClosure = Collections.emptySet();
            } else {
                transitiveClosure = queryable.query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet();
            }
        }

        Set<Entry<String, Map<Version, IInstallableUnit>>> s = slice.entrySet();
        final BigInteger POWER = BigInteger.valueOf(numberOfInstalledIUs > 0 ? numberOfInstalledIUs + 1 : 2);

        BigInteger maxWeight = POWER;
        for (Entry<String, Map<Version, IInstallableUnit>> entry : s) {
            List<IInstallableUnit> conflictingEntries = new ArrayList<>(entry.getValue().values());
            if (conflictingEntries.size() == 1) {
                //Only one IU exists with the namespace.
                IInstallableUnit iu = conflictingEntries.get(0);
                if (iu != metaIu) {
                    weightedObjects.add(WeightedObject.newWO(iu, POWER));
                }
                continue;
            }

            // Set the weight such that things that are already installed are not updated
            conflictingEntries.sort(Collections.reverseOrder());
            BigInteger weight = POWER;
            // have we already found a version that is already installed?
            boolean foundInstalled = false;
            // have we already found a version that is in the new roots?
            boolean foundRoot = false;
            for (IInstallableUnit iu : conflictingEntries) {
                if (!foundRoot && isInstalled(iu) && !transitiveClosure.contains(iu)) {
                    foundInstalled = true;
                    weightedObjects.add(WeightedObject.newWO(iu, BigInteger.ONE));
                } else if (!foundInstalled && !foundRoot && isRoot(iu, newRoots)) {
                    foundRoot = true;
                    weightedObjects.add(WeightedObject.newWO(iu, BigInteger.ONE));
                } else {
                    weightedObjects.add(WeightedObject.newWO(iu, weight));
                }
                weight = weight.multiply(POWER);
            }
            if (weight.compareTo(maxWeight) > 0)
                maxWeight = weight;
        }

        // no need to add one here, since maxWeight is strictly greater than the
        // maximal weight used so far.
        maxWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(s.size()));

        // Add the optional variables
        BigInteger optionalVarWeight = maxWeight.negate();
        for (AbstractVariable var : optionalRequirementVariable) {
            weightedObjects.add(WeightedObject.newWO(var, optionalVarWeight));
        }

        maxWeight = maxWeight.multiply(POWER).add(BigInteger.ONE);

        //Now we deal the optional IUs,
        long countOptional = 1;
        List<IInstallableUnit> requestedPatches = new ArrayList<>();
        for (IRequirement req : metaIu.getRequirements()) {
            if (req.getMin() > 0 || !req.isGreedy())
                continue;
            for (IInstallableUnit match : picker.query(QueryUtil.createMatchQuery(req.getMatches()), null)) {
                if (match instanceof IInstallableUnitPatch) {
                    requestedPatches.add(match);
                    countOptional = countOptional + 1;
                }
            }
        }

        // and we make sure that patches are always favored
        BigInteger patchWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(countOptional)).negate();
        for (IInstallableUnit iu : requestedPatches) {
            weightedObjects.add(WeightedObject.newWO(iu, patchWeight));
        }
        return weightedObjects;
    }

    protected boolean isInstalled(IInstallableUnit iu) {
        return !lastState.query(QueryUtil.createIUQuery(iu), null).isEmpty();
    }

    private boolean isRoot(IInstallableUnit iu, Collection<IInstallableUnit> newRoots) {
        return newRoots.contains(iu);
    }

    /**
     * Efficiently compute the size of a queryable
     */
    private int sizeOf(IQueryable<IInstallableUnit> installedIUs) {
        IQueryResult<IInstallableUnit> qr = installedIUs.query(QueryUtil.createIUAnyQuery(), null);
        if (qr instanceof Collector<?>)
            return ((Collector<?>) qr).size();
        return qr.toUnmodifiableSet().size();
    }

}
