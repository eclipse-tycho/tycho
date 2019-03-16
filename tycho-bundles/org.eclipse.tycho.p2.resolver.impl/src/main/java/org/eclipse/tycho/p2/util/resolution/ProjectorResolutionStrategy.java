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
package org.eclipse.tycho.p2.util.resolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.repository.p2base.metadata.QueryableCollection;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
public class ProjectorResolutionStrategy extends AbstractSlicerResolutionStrategy {

    public ProjectorResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    @Override
    protected Slicer newSlicer(IQueryable<IInstallableUnit> availableUnits, Map<String, String> properties) {
        return new TychoSlicer(availableUnits, properties, false);
    }

    // BUG 389698 : Add parent patch, we copy most of Slicer because private method
    private static class TychoSlicer extends Slicer {
        private static boolean DEBUG = false;
        private final IQueryable<IInstallableUnit> possibilites;
        private final boolean considerMetaRequirements;
        private final Map<String, Map<Version, IInstallableUnit>> slice; //The IUs that have been considered to be part of the problem
        private final MultiStatus result;

        private LinkedList<IInstallableUnit> toProcess;
        private Set<IInstallableUnit> considered; //IUs to add to the slice

        public TychoSlicer(IQueryable<IInstallableUnit> possibilites, Map<String, String> context,
                boolean considerMetaRequirements) {
            super(possibilites, context, considerMetaRequirements);
            this.possibilites = possibilites;
            this.considerMetaRequirements = considerMetaRequirements;
            slice = new HashMap<>();
            result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK,
                    Messages.Planner_Problems_resolving_plan, null);
        }

        @Override
        public IQueryable<IInstallableUnit> slice(IInstallableUnit[] ius, IProgressMonitor monitor) {
            considered = new HashSet<>(Arrays.asList(ius));
            toProcess = new LinkedList<>(considered);
            return super.slice(ius, monitor);
        }

        protected void processIU(IInstallableUnit iu) {
            iu = iu.unresolved();

            Map<Version, IInstallableUnit> iuSlice = slice.get(iu.getId());
            if (iuSlice == null) {

                iuSlice = new HashMap<>();
                slice.put(iu.getId(), iuSlice);
            }
            iuSlice.put(iu.getVersion(), iu);
            if (!isApplicable(iu)) {
                return;
            }

            Collection<IRequirement> reqs = getRequirements(iu);
            if (reqs.isEmpty())
                return;
            for (IRequirement req : reqs) {
                if (!isApplicable(req))
                    continue;

                if (!isGreedy(req)) {
                    continue;
                }

                expandRequirement(iu, req);
            }
            // BUG 389698 : Add parent patch
            if (iu instanceof InstallableUnitPatch) {
                IInstallableUnitPatch patchIU = (IInstallableUnitPatch) iu;
                IRequirement req = patchIU.getLifeCycle();
                if (isApplicable(req)) {
                    expandRequirement(iu, req);
                }
            }
        }

        protected boolean isGreedy(IRequirement req) {
            return req.isGreedy();
        }

        private Collection<IRequirement> getRequirements(IInstallableUnit iu) {
            boolean isPatch = iu instanceof IInstallableUnitPatch;
            boolean isFragment = iu instanceof IInstallableUnitFragment;
            //Short-circuit for the case of an IInstallableUnit 
            if ((!isFragment) && (!isPatch) && iu.getMetaRequirements().size() == 0)
                return iu.getRequirements();

            ArrayList<IRequirement> aggregatedRequirements = new ArrayList<>(
                    iu.getRequirements().size() + iu.getMetaRequirements().size()
                            + (isFragment ? ((IInstallableUnitFragment) iu).getHost().size() : 0)
                            + (isPatch ? ((IInstallableUnitPatch) iu).getRequirementsChange().size() : 0));
            aggregatedRequirements.addAll(iu.getRequirements());

            if (iu instanceof IInstallableUnitFragment) {
                aggregatedRequirements.addAll(((IInstallableUnitFragment) iu).getHost());
            }

            if (iu instanceof InstallableUnitPatch) {
                IInstallableUnitPatch patchIU = (IInstallableUnitPatch) iu;
                List<IRequirementChange> changes = patchIU.getRequirementsChange();
                for (int i = 0; i < changes.size(); i++) {
                    aggregatedRequirements.add(changes.get(i).newValue());
                }
            }

            if (considerMetaRequirements)
                aggregatedRequirements.addAll(iu.getMetaRequirements());
            return aggregatedRequirements;
        }

        private void expandRequirement(IInstallableUnit iu, IRequirement req) {
            if (req.getMax() == 0)
                return;
            IQueryResult<IInstallableUnit> matches = possibilites.query(QueryUtil.createMatchQuery(req.getMatches()),
                    null);
            int validMatches = 0;
            for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext();) {
                IInstallableUnit match = iterator.next();
                if (!isApplicable(match))
                    continue;
                validMatches++;
                Map<Version, IInstallableUnit> iuSlice = slice.get(match.getId());
                if (iuSlice == null || !iuSlice.containsKey(match.getVersion()))
                    consider(match);
            }

            if (validMatches == 0) {
                if (req.getMin() == 0) {
                    if (DEBUG)
                        System.out.println("No IU found to satisfy optional dependency of " + iu + " on req " + req); //$NON-NLS-1$//$NON-NLS-2$
                } else {
                    result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR,
                            NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
                }
            }
        }

        private void consider(IInstallableUnit match) {
            if (considered.add(match))
                toProcess.addLast(match);
        }

    }

    @Override
    protected boolean isSlicerError(MultiStatus slicerStatus) {
        return slicerStatus.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {

        Map<String, String> newSelectionContext = SimplePlanner.createSelectionContext(properties);

        IQueryable<IInstallableUnit> slice = slice(properties, monitor);

        Set<IInstallableUnit> seedUnits = new LinkedHashSet<>(data.getRootIUs());
        List<IRequirement> seedRequires = new ArrayList<>();
        if (data.getAdditionalRequirements() != null) {
            seedRequires.addAll(data.getAdditionalRequirements());
        }

        // force profile UIs to be used during resolution
        seedUnits.addAll(data.getEEResolutionHints().getMandatoryUnits());
        seedRequires.addAll(data.getEEResolutionHints().getMandatoryRequires());

        Projector projector = new Projector(slice, newSelectionContext, new HashSet<IInstallableUnit>(), false);
        projector.encode(createUnitRequiring("tycho", seedUnits, seedRequires),
                EMPTY_IU_ARRAY /* alreadyExistingRoots */,
                new QueryableArray(EMPTY_IU_ARRAY) /* installedIUs */, seedUnits /* newRoots */, monitor);
        IStatus s = projector.invokeSolver(monitor);
        if (s.getSeverity() == IStatus.ERROR) {
            // log all transitive requirements which cannot be satisfied; this doesn't print the dependency chain from the seed to the units with missing requirements, so this is less useful than the "explanation" 
            logger.debug(StatusTool.collectProblems(s));

            Set<Explanation> explanation = projector.getExplanation(new NullProgressMonitor()); // suppress "Cannot complete the request.  Generating details."
            throw new ResolverException(toString(explanation), newSelectionContext.toString(),
                    StatusTool.findException(s));
        }
        Collection<IInstallableUnit> newState = projector.extractSolution();

        // remove fake IUs from resolved state
        newState.removeAll(data.getEEResolutionHints().getTemporaryAdditions());

        fixSWT(data.getAvailableIUs(), newState, newSelectionContext, monitor);

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(newState, false));
        }

        return newState;
    }

    private String toString(Set<Explanation> explanation) {
        StringBuilder result = new StringBuilder();
        for (Explanation explanationLine : explanation) {
            result.append(explanationLine.toString());
            result.append('\n');
        }
        return result.substring(0, result.length() - 1);
    }

    /*
     * workaround for SWT bug 361901: bundles generally require org.eclipse.swt, but this is an
     * empty shell and only native fragments of org.eclipse.swt provide classes to compile against.
     * There is no dependency from the host to the fragments, so we need to add the matching SWT
     * fragment manually.
     */
    void fixSWT(Collection<IInstallableUnit> availableIUs, Collection<IInstallableUnit> resolutionResult,
            Map<String, String> newSelectionContext, IProgressMonitor monitor) {
        IInstallableUnit swtHost = findSWTHostIU(resolutionResult);

        if (swtHost == null) {
            return;
        } else if (swtHost.getVersion().compareTo(Version.createOSGi(3, 104, 0)) >= 0) {
            // bug 361901 was fixed in Mars
            return;
        }

        // 380934 one of rootIUs can be SWT or an SWT fragment
        for (IInstallableUnit iu : data.getRootIUs()) {
            if ("org.eclipse.swt".equals(iu.getId())) {
                return;
            }
            for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
                if ("osgi.fragment".equals(provided.getNamespace()) && "org.eclipse.swt".equals(provided.getName())) {
                    return;
                }
            }
        }

        IInstallableUnit swtFragment = null;

        all_ius: for (Iterator<IInstallableUnit> iter = new QueryableCollection(availableIUs)
                .query(QueryUtil.ALL_UNITS, monitor).iterator(); iter.hasNext();) {
            IInstallableUnit iu = iter.next();
            if (iu.getId().startsWith("org.eclipse.swt") && isApplicable(newSelectionContext, iu.getFilter())
                    && providesJavaPackages(iu)) {
                for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
                    if ("osgi.fragment".equals(provided.getNamespace())
                            && "org.eclipse.swt".equals(provided.getName())) {
                        if (swtFragment == null || swtFragment.getVersion().compareTo(iu.getVersion()) < 0) {
                            swtFragment = iu;
                        }
                        continue all_ius;
                    }
                }
            }
        }

        if (swtFragment == null) {
            throw new RuntimeException(
                    "Could not determine SWT implementation fragment bundle for environment " + newSelectionContext);
        }

        resolutionResult.add(swtFragment);
    }

    private IInstallableUnit findSWTHostIU(Collection<IInstallableUnit> ius) {
        for (IInstallableUnit iu : ius) {
            if ("org.eclipse.swt".equals(iu.getId())) {
                return iu;
            }
        }
        return null;
    }

    private boolean providesJavaPackages(IInstallableUnit iu) {
        for (IProvidedCapability capability : iu.getProvidedCapabilities()) {
            if ("java.package".equals(capability.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isApplicable(Map<String, String> selectionContext, IMatchExpression<IInstallableUnit> filter) {
        if (filter == null) {
            return true;
        }

        return filter.isMatch(InstallableUnit.contextIU(selectionContext));
    }

}
