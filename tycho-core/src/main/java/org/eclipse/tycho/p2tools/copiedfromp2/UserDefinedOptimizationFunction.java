/*******************************************************************************
 * Copyright (c) 2009, 2020 Daniel Le Berre and others.
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
 *     Daniel Le Berre - initial API and implementation
 *     Red Hat, Inc. - support for remediation page
 ******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2tools.copiedfromp2.Projector.AbstractVariable;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.SteppedTimeoutLexicoHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.ContradictionException;

public class UserDefinedOptimizationFunction extends OptimizationFunction {
    private Collection<IInstallableUnit> alreadyExistingRoots;
    private SteppedTimeoutLexicoHelper<Object, Explanation> dependencyHelper;
    private IQueryable<IInstallableUnit> picker;

    public UserDefinedOptimizationFunction(IQueryable<IInstallableUnit> lastState,
            List<AbstractVariable> abstractVariables, List<AbstractVariable> optionalVariables,
            IQueryable<IInstallableUnit> picker, IInstallableUnit selectionContext,
            Map<String, Map<Version, IInstallableUnit>> slice, DependencyHelper<Object, Explanation> dependencyHelper,
            Collection<IInstallableUnit> alreadyInstalledIUs) {
        super(lastState, abstractVariables, optionalVariables, picker, selectionContext, slice);
        this.picker = picker;
        this.slice = slice;
        this.dependencyHelper = (SteppedTimeoutLexicoHelper<Object, Explanation>) dependencyHelper;
        this.alreadyExistingRoots = alreadyInstalledIUs;
    }

    @Override
    public List<WeightedObject<? extends Object>> createOptimizationFunction(IInstallableUnit metaIu,
            Collection<IInstallableUnit> newRoots) {
        List<WeightedObject<?>> weightedObjects = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        BigInteger weight = BigInteger.valueOf(slice.size() + 1);
        String[] criterias = new String[] { "+new", "-notuptodate", "-changed", "-removed" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
        BigInteger currentWeight = weight.pow(criterias.length - 1);
        boolean maximizes;
        Object thing;
        for (String criteria : criterias) {
            if (criteria.endsWith("new")) { //$NON-NLS-1$
                weightedObjects.clear();
                newRoots(weightedObjects, criteria.startsWith("+") ? currentWeight.negate() : currentWeight, metaIu); //$NON-NLS-1$
                currentWeight = currentWeight.divide(weight);
            } else if (criteria.endsWith("removed")) { //$NON-NLS-1$
                weightedObjects.clear();
                removedRoots(weightedObjects, criteria.startsWith("+") ? currentWeight.negate() : currentWeight, //$NON-NLS-1$
                        metaIu);
                currentWeight = currentWeight.divide(weight);
            } else if (criteria.endsWith("notuptodate")) { //$NON-NLS-1$
                weightedObjects.clear();
                notuptodate(weightedObjects, criteria.startsWith("+") ? currentWeight.negate() : currentWeight, metaIu); //$NON-NLS-1$
                currentWeight = currentWeight.divide(weight);
            } else if (criteria.endsWith("changed")) { //$NON-NLS-1$
                weightedObjects.clear();
                changedRoots(weightedObjects, criteria.startsWith("+") ? currentWeight.negate() : currentWeight, //$NON-NLS-1$
                        metaIu);
                currentWeight = currentWeight.divide(weight);
            }
            objects.clear();
            maximizes = criteria.startsWith("+"); //$NON-NLS-1$
            for (WeightedObject<?> weightedObject : weightedObjects) {
                thing = weightedObject.thing;
                if (maximizes) {
                    thing = dependencyHelper.not(thing);
                }
                objects.add(thing);
            }
            dependencyHelper.addCriterion(objects);
        }
        weightedObjects.clear();
        return null;
    }

    protected void changedRoots(List<WeightedObject<?>> weightedObjects, BigInteger weight,
            IInstallableUnit entryPointIU) {
        Collection<IRequirement> requirements = entryPointIU.getRequirements();
        for (IRequirement req : requirements) {
            IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(req.getMatches());
            IQueryResult<IInstallableUnit> matches = picker.query(query, null);
            Object[] changed = new Object[matches.toUnmodifiableSet().size()];
            int i = 0;
            for (IInstallableUnit match : matches) {
                changed[i++] = isInstalledAsRoot(match) ? dependencyHelper.not(match) : match;
            }
            try {
                Projector.AbstractVariable abs = new Projector.AbstractVariable("CHANGED"); //$NON-NLS-1$
                dependencyHelper.or(FakeExplanation.getInstance(), abs, changed);
                weightedObjects.add(WeightedObject.newWO(abs, weight));
            } catch (ContradictionException e) {
                // TODO Auto-generated catch block TODO
                e.printStackTrace();
            }
        }
    }

    protected void newRoots(List<WeightedObject<?>> weightedObjects, BigInteger weight, IInstallableUnit entryPointIU) {
        Collection<IRequirement> requirements = entryPointIU.getRequirements();
        for (IRequirement req : requirements) {
            IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(req.getMatches());
            IQueryResult<IInstallableUnit> matches = picker.query(query, null);
            boolean oneInstalled = false;
            for (IInstallableUnit match : matches) {
                oneInstalled = oneInstalled || isInstalledAsRoot(match);
            }
            if (!oneInstalled) {
                try {
                    Projector.AbstractVariable abs = new Projector.AbstractVariable("NEW"); //$NON-NLS-1$
                    dependencyHelper.or(FakeExplanation.getInstance(), abs,
                            (Object[]) matches.toArray(IInstallableUnit.class));
                    weightedObjects.add(WeightedObject.newWO(abs, weight));
                } catch (ContradictionException e) {
                    // should not happen
                    e.printStackTrace();
                }
            }
        }
    }

    protected void removedRoots(List<WeightedObject<?>> weightedObjects, BigInteger weight,
            IInstallableUnit entryPointIU) {
        Collection<IRequirement> requirements = entryPointIU.getRequirements();
        for (IRequirement req : requirements) {
            IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(req.getMatches());
            IQueryResult<IInstallableUnit> matches = picker.query(query, null);
            boolean installed = false;
            Object[] literals = new Object[matches.toUnmodifiableSet().size()];
            int i = 0;
            for (IInstallableUnit match : matches) {
                installed = installed || isInstalledAsRoot(match);
                literals[i++] = dependencyHelper.not(match);
            }
            if (installed) {
                try {
                    Projector.AbstractVariable abs = new Projector.AbstractVariable("REMOVED"); //$NON-NLS-1$
                    dependencyHelper.and(FakeExplanation.getInstance(), abs, literals);
                    weightedObjects.add(WeightedObject.newWO(abs, weight));
                } catch (ContradictionException e) {
                    // should not happen TODO
                    e.printStackTrace();
                }
            }
        }
    }

    protected void notuptodate(List<WeightedObject<?>> weightedObjects, BigInteger weight,
            IInstallableUnit entryPointIU) {
        Collection<IRequirement> requirements = entryPointIU.getRequirements();
        for (IRequirement req : requirements) {
            IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(req.getMatches());
            IQueryResult<IInstallableUnit> matches = picker.query(query, null);
            List<IInstallableUnit> toSort = new ArrayList<>(matches.toUnmodifiableSet());
            toSort.sort(Collections.reverseOrder());
            if (toSort.isEmpty())
                continue;

            Projector.AbstractVariable abs = new Projector.AbstractVariable();
            Object notlatest = dependencyHelper.not(toSort.get(0));
            try {
                // notuptodate <=> not iuvn and (iuv1 or iuv2 or ... iuvn-1)
                dependencyHelper.implication(new Object[] { abs }).implies(notlatest)
                        .named(FakeExplanation.getInstance());
                Object[] clause = new Object[toSort.size()];
                toSort.toArray(clause);
                clause[0] = dependencyHelper.not(abs);
                dependencyHelper.clause(FakeExplanation.getInstance(), clause);
                for (int i = 1; i < toSort.size(); i++) {
                    dependencyHelper.implication(new Object[] { notlatest, toSort.get(i) }).implies(abs)
                            .named(FakeExplanation.getInstance());
                }
            } catch (ContradictionException e) {
                // should never happen
                e.printStackTrace();
            }

            weightedObjects.add(WeightedObject.newWO(abs, weight));
        }
    }

    private static class FakeExplanation extends Explanation {
        private static Explanation singleton = new FakeExplanation();

        public static Explanation getInstance() {
            return singleton;
        }

        @Override
        protected int orderValue() {
            return Explanation.OTHER_REASON;
        }

        @Override
        public int shortAnswer() {
            return 0;
        }

    }

    private boolean isInstalledAsRoot(IInstallableUnit isInstalled) {
        for (IInstallableUnit installed : alreadyExistingRoots) {
            if (isInstalled.equals(installed))
                return true;
        }
        return false;
    }

}
