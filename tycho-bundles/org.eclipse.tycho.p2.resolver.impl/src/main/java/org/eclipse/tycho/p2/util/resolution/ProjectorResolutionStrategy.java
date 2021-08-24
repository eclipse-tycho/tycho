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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
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
        return new Slicer(availableUnits, properties, false);
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
                EMPTY_IU_ARRAY /* alreadyExistingRoots */, new QueryableArray(EMPTY_IU_ARRAY) /* installedIUs */,
                seedUnits /* newRoots */, monitor);
        IStatus s = projector.invokeSolver(monitor);
        if (s.getSeverity() == IStatus.ERROR) {
            // log all transitive requirements which cannot be satisfied; this doesn't print the dependency chain from the seed to the units with missing requirements, so this is less useful than the "explanation" 
            logger.debug(StatusTool.collectProblems(s));

            Set<Explanation> explanation = projector.getExplanation(new NullProgressMonitor()); // suppress "Cannot complete the request.  Generating details."
            throw new ResolverException(explanation.stream().map(Object::toString).collect(Collectors.joining("\n")),
                    newSelectionContext.toString(), StatusTool.findException(s));
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

        all_ius: for (Iterator<IInstallableUnit> iter = new QueryableCollection(availableIUs).query(
                QueryUtil.ALL_UNITS, monitor).iterator(); iter.hasNext();) {
            IInstallableUnit iu = iter.next();
            if (iu.getId().startsWith("org.eclipse.swt") && isApplicable(newSelectionContext, iu.getFilter())
                    && providesJavaPackages(iu)) {
                for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
                    if ("osgi.fragment".equals(provided.getNamespace()) && "org.eclipse.swt".equals(provided.getName())) {
                        if (swtFragment == null || swtFragment.getVersion().compareTo(iu.getVersion()) < 0) {
                            swtFragment = iu;
                        }
                        continue all_ius;
                    }
                }
            }
        }

        if (swtFragment == null) {
            throw new RuntimeException("Could not determine SWT implementation fragment bundle for environment "
                    + newSelectionContext);
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
