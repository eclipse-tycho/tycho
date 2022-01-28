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
package org.eclipse.tycho.p2.util.resolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
public class ProjectorResolutionStrategy extends AbstractSlicerResolutionStrategy {
    /**
     * Internal property to control the maximum number of iterations performed to resolve an
     * incomplete solution
     */
    private static final int MAX_ITERATIONS = Integer
            .getInteger("tycho.internal.ProjectorResolutionStrategy.maxIterations", 1000);

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

        int iteration = 0;
        do {
            Projector projector = new Projector(slice(properties, generatedUnits, monitor), selectionContext,
                    new HashSet<IInstallableUnit>(), false);
            projector.encode(createUnitRequiring("tycho", seedUnits, seedRequires),
                    EMPTY_IU_ARRAY /* alreadyExistingRoots */,
                    new QueryableArray(EMPTY_IU_ARRAY) /* installedIUs */, seedUnits /* newRoots */, monitor);
            IStatus s = projector.invokeSolver(monitor);
            if (s.getSeverity() == IStatus.ERROR) {
                Set<Explanation> explanation = projector.getExplanation(new NullProgressMonitor()); // suppress "Cannot complete the request.  Generating details."
                if (!data.failOnMissingRequirements()) {
                    List<IRequirement> missingRequirements = computeMissingRequirements(explanation);
                    if (missingRequirements.size() > 0) {
                        if (logger.isExtendedDebugEnabled()) {
                            logger.debug(
                                    "At iteration " + iteration + " the following requirements are not yet satisfied:");
                            for (IRequirement requirement : missingRequirements) {
                                logger.debug("> " + requirement);
                            }
                        }
                        //only start a new resolve if we have collected additional requirements...
                        IInstallableUnit providing = createUnitProviding("tycho.unresolved.requirements",
                                missingRequirements);
                        int newCapabilities = providing.getProvidedCapabilities().size();
                        if (newCapabilities > 0) {
                            //... and we could provide additional capabilities
                            if (logger.isExtendedDebugEnabled()) {
                                logger.debug(newCapabilities
                                        + " new capabilities where created, starting next iteration...");
                            }
                            generatedUnits.add(providing);
                            iteration++;
                            continue;
                        }
                    }
                }
                // log all transitive requirements which cannot be satisfied; this doesn't print the dependency chain from the seed to the units with missing requirements, so this is less useful than the "explanation" 
                logger.debug(StatusTool.collectProblems(s));
                explainProblems(explanation, MavenLogger::error);
                throw new ResolverException(
                        explanation.stream().map(Object::toString).collect(Collectors.joining("\n")),
                        selectionContext.toString(), StatusTool.findException(s));
            }
            Collection<IInstallableUnit> newState = projector.extractSolution();

            // remove fake IUs from resolved state
            newState.removeAll(data.getEEResolutionHints().getTemporaryAdditions());
            newState.removeAll(generatedUnits); //remove the tycho generated IUs if any

            if (logger.isExtendedDebugEnabled()) {
                logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(newState, false));
            }
            return newState;
        } while (iteration < MAX_ITERATIONS);
        throw new ResolverException("Maximum iterations reached", new TimeoutException());
    }

}
