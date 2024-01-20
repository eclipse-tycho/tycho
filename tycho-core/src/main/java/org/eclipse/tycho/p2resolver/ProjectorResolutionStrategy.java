/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - #462 - Delay Pom considered items to the final Target Platform calculation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2tools.copiedfromp2.Slicer;

public class ProjectorResolutionStrategy extends AbstractSlicerResolutionStrategy {

    public ProjectorResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    @Override
    protected Slicer newSlicer(IQueryable<IInstallableUnit> availableUnits, Map<String, String> properties) {
        Predicate<IInstallableUnit> acceptor = data.getIInstallableUnitAcceptor();
        return new Slicer(availableUnits, properties, false) {
            @Override
            protected boolean isApplicable(IInstallableUnit iu) {
                if (acceptor != null) {
                    return acceptor.test(iu);
                }
                return super.isApplicable(iu);
            }
        };
    }

    @Override
    protected boolean isSlicerError(MultiStatus slicerStatus) {
        return slicerStatus.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    @Override
    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {
        List<IInstallableUnit> generatedUnits = new ArrayList<>();
        Map<String, String> selectionContext = SimplePlanner.createSelectionContext(properties);

        Set<IInstallableUnit> seedUnits = new LinkedHashSet<>(data.getRootIUs());
        List<IRequirement> seedRequires = new ArrayList<>();
        if (data.getAdditionalRequirements() != null) {
            seedRequires.addAll(data.getAdditionalRequirements());
        }

        // force profile UIs to be used during resolution
        seedUnits.addAll(data.getEEResolutionHints().getMandatoryUnits());
        seedRequires.addAll(data.getEEResolutionHints().getMandatoryRequires());

        Projector projector = new Projector(slice(properties, generatedUnits, monitor), selectionContext,
                new HashSet<>(), false);
        projector.encode(createUnitRequiring("tycho", seedUnits, seedRequires),
                EMPTY_IU_ARRAY /* alreadyExistingRoots */,
                new QueryableArray(List.of()) /* installedIUs */, seedUnits /* newRoots */, monitor);
        IStatus s = projector.invokeSolver(monitor);
        if (s.getSeverity() == IStatus.ERROR) {
            Set<Explanation> explanation = getExplanation(projector); // suppress "Cannot complete the request.  Generating details."
            // log all transitive requirements which cannot be satisfied; this doesn't print the dependency chain from the seed to the units with missing requirements, so this is less useful than the "explanation"
            logger.debug(StatusTool.toLogMessage(s));
            explainProblems(explanation, MavenLogger::error);
            throw new ResolverException(explanation, selectionContext.toString(), StatusTool.findException(s));
        }
        if (s.getSeverity() == IStatus.WARNING) {
            logger.warn(StatusTool.toLogMessage(s));
        }
        Collection<IInstallableUnit> newState = projector.extractSolution();

        // remove fake IUs from resolved state
        newState.removeAll(data.getEEResolutionHints().getTemporaryAdditions());
        newState.removeAll(generatedUnits); //remove the tycho generated IUs if any

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(newState, false));
        }
        return newState;
    }

    private Set<Explanation> getExplanation(Projector projector) {
        try {
            return projector.getExplanation(new NullProgressMonitor());
        } catch (IllegalStateException e) {
            //this schedules a job, if this fails (e.g. in tests) we simply use no explanation
            return Set.of();
        }
    }

}
