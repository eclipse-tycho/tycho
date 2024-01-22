/*******************************************************************************
 *  Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Sonatype, Inc. - ongoing development
 *      Christoph Läubrich - Issue #39 - Slicer should allow filtering of a requirement based on the IU currently processed
 *******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;

public class Slicer {
    private static boolean DEBUG = false;
    private final IQueryable<IInstallableUnit> possibilites;
    private final boolean considerMetaRequirements;
    protected final IInstallableUnit selectionContext;
    /** The IUs that have been considered to be part of the problem */
    private final Map<String, Map<Version, IInstallableUnit>> slice = new HashMap<>();
    private final MultiStatus result = new MultiStatus(Slicer.class, 0, Messages.Planner_Problems_resolving_plan);

    private Queue<IInstallableUnit> toProcess;
    private Set<IInstallableUnit> considered; // IUs to add to the slice
    private final Set<IInstallableUnit> nonGreedyIUs = new HashSet<>(); // IUs that are brought in by non greedy dependencies

    public Slicer(IQueryable<IInstallableUnit> input, Map<String, String> context, boolean considerMetaRequirements) {
        this(input, InstallableUnit.contextIU(context), considerMetaRequirements);
    }

    public Slicer(IQueryable<IInstallableUnit> possibilites, IInstallableUnit selectionContext,
            boolean considerMetaRequirements) {
        this.possibilites = possibilites;
        this.selectionContext = selectionContext;
        this.considerMetaRequirements = considerMetaRequirements;
    }

    public IQueryable<IInstallableUnit> slice(Collection<IInstallableUnit> ius, IProgressMonitor monitor) {
        monitor = IProgressMonitor.nullSafe(monitor);
        try {
            long start = 0;
            if (DEBUG) {
                start = System.currentTimeMillis();
                System.out.println("Start slicing: " + start); //$NON-NLS-1$
            }
            validateInput(ius);
            considered = new HashSet<>(ius);
            toProcess = new LinkedList<>(considered);
            while (!toProcess.isEmpty()) {
                if (monitor.isCanceled()) {
                    result.merge(Status.CANCEL_STATUS);
                    throw new OperationCanceledException();
                }
                processIU(toProcess.remove());
            }
            computeNonGreedyIUs();
            if (DEBUG) {
                long stop = System.currentTimeMillis();
                System.out.println("Slicing complete: " + (stop - start)); //$NON-NLS-1$
            }
        } catch (IllegalStateException e) {
            result.add(Status.error(e.getMessage(), e));
        }
        if (Tracing.DEBUG && result.getSeverity() != IStatus.OK) {
            LogHelper.log(result);
        }
        if (result.getSeverity() == IStatus.ERROR) {
            return null;
        }
        return new QueryableArray(considered, false);
    }

    private void computeNonGreedyIUs() {
        IQueryable<IInstallableUnit> queryable = new QueryableArray(considered, false);
        for (IInstallableUnit iu : queryable.query(QueryUtil.ALL_UNITS, new NullProgressMonitor())) {
            iu = iu.unresolved();
            Collection<IRequirement> reqs = getRequirements(iu);
            for (IRequirement req : reqs) {
                if (!isApplicable(iu, req)) {
                    continue;
                }
                if (!isGreedy(iu, req)) {
                    nonGreedyIUs.addAll(
                            queryable.query(QueryUtil.createMatchQuery(req.getMatches()), null).toUnmodifiableSet());
                }
            }
        }
    }

    public MultiStatus getStatus() {
        return result;
    }

    // This is a shortcut to simplify the error reporting when the filter of the ius
    // we are being asked to install does not pass
    private void validateInput(Collection<IInstallableUnit> ius) {
        for (IInstallableUnit iu : ius) {
            if (!isApplicable(iu)) {
                throw new IllegalStateException(NLS.bind(Messages.Explanation_missingRootFilter, iu));
            }
        }
    }

    // Check whether the requirement is applicable

    protected boolean isApplicable(IInstallableUnit unit, IRequirement req) {
        return isApplicable(req);
    }

    protected boolean isApplicable(IRequirement req) {
        IMatchExpression<IInstallableUnit> filter = req.getFilter();
        return filter == null || filter.isMatch(selectionContext);
    }

    protected boolean isApplicable(IInstallableUnit iu) {
        IMatchExpression<IInstallableUnit> filter = iu.getFilter();
        return filter == null || filter.isMatch(selectionContext);
    }

    protected void processIU(IInstallableUnit iu) {
        iu = iu.unresolved();
        Map<Version, IInstallableUnit> iuSlice = slice.computeIfAbsent(iu.getId(), i -> new HashMap<>());
        iuSlice.put(iu.getVersion(), iu);
        if (!isApplicable(iu)) {
            return;
        }
        Collection<IRequirement> reqs = getRequirements(iu);
        for (IRequirement req : reqs) {
            if (isApplicable(iu, req) && isGreedy(iu, req)) {
                expandRequirement(iu, req);
            }
        }
    }

    protected boolean isGreedy(IInstallableUnit unit, IRequirement req) {
        return isGreedy(req);
    }

    protected boolean isGreedy(IRequirement req) {
        return req.isGreedy();
    }

    private Collection<IRequirement> getRequirements(IInstallableUnit iu) {
        boolean isPatch = iu instanceof IInstallableUnitPatch;
        boolean isFragment = iu instanceof IInstallableUnitFragment;
        // Short-circuit for the case of an IInstallableUnit
        if (!isFragment && !isPatch && iu.getMetaRequirements().isEmpty()) {
            return iu.getRequirements();
        }

        List<IRequirement> aggregatedRequirements = new ArrayList<>(iu.getRequirements().size()
                + iu.getMetaRequirements().size() + (isFragment ? ((IInstallableUnitFragment) iu).getHost().size() : 0)
                + (isPatch ? ((IInstallableUnitPatch) iu).getRequirementsChange().size() : 0));
        aggregatedRequirements.addAll(iu.getRequirements());

        if (iu instanceof IInstallableUnitFragment iuFragment) {
            aggregatedRequirements.addAll(iuFragment.getHost());
        }
        if (iu instanceof InstallableUnitPatch patchIU) {
            List<IRequirementChange> changes = patchIU.getRequirementsChange();
            for (IRequirementChange change : changes) {
                aggregatedRequirements.add(change.newValue());
            }
        }
        if (considerMetaRequirements) {
            aggregatedRequirements.addAll(iu.getMetaRequirements());
        }
        return aggregatedRequirements;
    }

    private Set<IRequirement> consideredRequirements = new HashSet<>();

    private void expandRequirement(IInstallableUnit iu, IRequirement req) {
        if (req.getMax() == 0) {
            return;
        }
        if (consideredRequirements.add(req)) {
            List<IInstallableUnit> selected = selectIUsForRequirement(possibilites, req).toList();
            for (IInstallableUnit match : selected) {
                Map<Version, IInstallableUnit> iuSlice = slice.get(match.getId());
                if ((iuSlice == null || !iuSlice.containsKey(match.getVersion())) && considered.add(match)) {
                    toProcess.add(match);
                }
            }
            if (selected.isEmpty()) {
                if (req.getMin() == 0) {
                    if (DEBUG) {
                        System.out.println("No IU found to satisfy optional dependency of " + iu + " on req " + req); //$NON-NLS-1$//$NON-NLS-2$
                    }
                } else {
                    result.add(Status.warning(NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
                }
            }
        }
    }

    protected Stream<IInstallableUnit> selectIUsForRequirement(IQueryable<IInstallableUnit> queryable,
            IRequirement req) {
        return queryable.query(QueryUtil.createMatchQuery(req.getMatches()), null).stream().filter(this::isApplicable);
    }

    Set<IInstallableUnit> getNonGreedyIUs() {
        return nonGreedyIUs;
    }
}
