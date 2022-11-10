/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Bug 538144] Support other target locations (Directory, Features, Installations) 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionResolutionException;
import org.eclipse.tycho.targetplatform.TargetDefinitionSyntaxException;

public class InstallableUnitResolver {

    private static final String SOURCE_IU_ID = "org.eclipse.tycho.internal.target.source.bundles";

    private IncludeMode includeMode = null;
    private Boolean includeAllEnvironments = null;
    private Boolean includeSource = null;

    private List<TargetEnvironment> environments;

    private ExecutionEnvironmentResolutionHints executionEnvironment;

    private MavenLogger logger;

    private List<RootUnits> rootUnits = new ArrayList<>();

    private IncludeSourceMode sourceMode;

    public InstallableUnitResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IncludeSourceMode sourceMode,
            MavenLogger logger) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.sourceMode = sourceMode;
        this.logger = logger;
    }

    public void addLocation(InstallableUnitLocation iuLocationDefinition, IQueryable<IInstallableUnit> localUnits) {
        //update (and validate) desired global state
        setIncludeMode(iuLocationDefinition.getIncludeMode());
        setIncludeAllEnvironments(iuLocationDefinition.includeAllEnvironments());
        setIncludeSource(switch (sourceMode) {
        case force -> true;
        case ignore -> false;
        default -> iuLocationDefinition.includeSource();
        });
        //resolve root units and add them
        rootUnits.add(new RootUnits(getRootIUs(iuLocationDefinition.getUnits(), localUnits), localUnits));
    }

    private void setIncludeMode(IncludeMode newValue) throws TargetDefinitionResolutionException {
        if (includeMode != newValue) {
            if (includeMode != null) {
                throw new TargetDefinitionResolutionException("Include mode must be the same for all locations");
            }
            includeMode = newValue;
        }
    }

    private void setIncludeAllEnvironments(Boolean newValue) throws TargetDefinitionResolutionException {
        if (!newValue.equals(includeAllEnvironments)) {
            if (includeAllEnvironments != null) {
                throw new TargetDefinitionResolutionException(
                        "The attribute 'includeAllPlatforms' must be the same for all locations");
            }
            includeAllEnvironments = newValue;
        }
    }

    private void setIncludeSource(Boolean newValue) {
        if (!newValue.equals(includeSource)) {
            if (includeSource != null) {
                throw new TargetDefinitionResolutionException(
                        "The attribute 'includeSource' must be the same for all locations");
            }
            includeSource = newValue;
        }
    }

    public IQueryResult<IInstallableUnit> resolve(IQueryable<IInstallableUnit> allUnits) throws ResolverException {
        UnitCollector collector = new UnitCollector();
        if (haveContent()) {
            if (includeMode == IncludeMode.PLANNER) {
                //resolve as one bulk
                Set<IInstallableUnit> allRoots = new HashSet<>();
                for (RootUnits root : rootUnits) {
                    allRoots.addAll(root.rootIUs);
                }
                ResolutionDataImpl data = new ResolutionDataImpl(executionEnvironment);
                data.setRootIUs(allRoots);
                data.setAvailableIUsAndFilter(allUnits);
                Collection<IInstallableUnit> resolve = getPlannerResolutionStrategy(data)
                        .multiPlatformResolve(environments, new DuplicateFilteringLoggingProgressMonitor(logger));
                if (!resolve.isEmpty()) {
                    collector.addAll(resolve);
                    if (includeSource) {
                        collector.addAll(addSourceBundleUnits(data, this::getPlannerResolutionStrategy, resolve,
                                new DuplicateFilteringLoggingProgressMonitor(logger)));
                    }
                }
            } else {
                //resolve every locations as its own
                for (RootUnits root : rootUnits) {
                    ResolutionDataImpl data = new ResolutionDataImpl(executionEnvironment);
                    data.setRootIUs(root.rootIUs);
                    data.setAvailableIUsAndFilter(root.localUnits);
                    SlicerResolutionStrategy strategy = getSlicerResolutionStrategy(data, true);
                    Collection<IInstallableUnit> resolve = strategy.multiPlatformResolve(environments,
                            new DuplicateFilteringLoggingProgressMonitor(logger));
                    if (!resolve.isEmpty()) {
                        collector.addAll(resolve);
                        if (includeSource) {
                            collector.addAll(addSourceBundleUnits(data, d -> getSlicerResolutionStrategy(d, false),
                                    resolve, new DuplicateFilteringLoggingProgressMonitor(logger)));
                        }
                    }
                }

            }
        }
        return collector;
    }

    private boolean haveContent() {
        for (RootUnits root : rootUnits) {
            if (root.rootIUs.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private SlicerResolutionStrategy getSlicerResolutionStrategy(ResolutionData data, boolean warn) {
        SlicerResolutionStrategy strategy = new SlicerResolutionStrategy(logger, includeAllEnvironments, warn);
        strategy.setData(data);
        return strategy;
    }

    private ProjectorResolutionStrategy getPlannerResolutionStrategy(ResolutionData data)
            throws TargetDefinitionResolutionException {
        if (includeAllEnvironments) {
            logger.warn(
                    "includeAllPlatforms='true' and includeMode='planner' are incompatible. ignore includeAllPlatforms flag");
        }
        ProjectorResolutionStrategy strategy = new ProjectorResolutionStrategy(logger);
        strategy.setData(data);
        return strategy;
    }

    private static final class RootUnits {

        private Collection<IInstallableUnit> rootIUs;
        private IQueryable<IInstallableUnit> localUnits;

        public RootUnits(Collection<IInstallableUnit> rootIUs, IQueryable<IInstallableUnit> localUnits) {
            this.rootIUs = rootIUs;
            this.localUnits = localUnits;
        }

    }

    private static class UnitCollector extends Collector<IInstallableUnit> {

        public void addAll(Collection<? extends IInstallableUnit> units) {
            for (IInstallableUnit unit : units) {
                accept(unit);
            }
        }
    }

    private static Collection<IInstallableUnit> addSourceBundleUnits(ResolutionDataImpl data,
            Function<ResolutionData, AbstractResolutionStrategy> strategySupplier, Collection<IInstallableUnit> units,
            IProgressMonitor progressMonitor) throws ResolverException {
        // see org.eclipse.pde.internal.core.target.P2TargetUtils#createSourceIU()
        final IRequirement bundleRequirement = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type",
                "bundle", null, null, false, false, false);
        ArrayList<IRequirement> sourceBundleRequirements = new ArrayList<>();
        for (IInstallableUnit unit : units) {
            if (unit.satisfies(bundleRequirement)) {
                final VersionRange perfectVersionMatch = new VersionRange(unit.getVersion(), true, unit.getVersion(),
                        true);
                IRequirement optionalGreedySourceBundleRequirement = MetadataFactory.createRequirement("osgi.bundle",
                        unit.getId() + ".source", perfectVersionMatch, null, true, false, true);
                sourceBundleRequirements.add(optionalGreedySourceBundleRequirement);
            }
        }
        InstallableUnitDescription sourceDescription = new MetadataFactory.InstallableUnitDescription();
        sourceDescription.setId(SOURCE_IU_ID);
        final Version sourceIUVersion = Version.createOSGi(1, 0, 0);
        sourceDescription.setVersion(sourceIUVersion);
        IProvidedCapability capability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID,
                SOURCE_IU_ID, sourceIUVersion);
        sourceDescription.setCapabilities(new IProvidedCapability[] { capability });
        sourceDescription.addRequirements(sourceBundleRequirements);

        IInstallableUnit sourceIU = MetadataFactory.createInstallableUnit(sourceDescription);
        Collection<IInstallableUnit> oldUis = data.getRootIUs();
        try {
            data.setRootIUs(Collections.singleton(sourceIU));
            final TargetEnvironment nonFilteringEnvironment = new TargetEnvironment();
            Collection<IInstallableUnit> sourceUnits = strategySupplier.apply(data).resolve(nonFilteringEnvironment,
                    progressMonitor);
            sourceUnits.remove(sourceIU); // nobody wants to see our artificial IU
            return sourceUnits; // TODO: remove duplicates?
        } finally {
            data.setRootIUs(oldUis);
        }
    }

    private static Collection<IInstallableUnit> getRootIUs(Collection<? extends Unit> unitReferences,
            IQueryable<IInstallableUnit> queryable) {
        List<IInstallableUnit> result = new ArrayList<>();
        for (Unit unitReference : unitReferences) {
            result.add(findUnits(unitReference, queryable));
        }
        return result;
    }

    private static IInstallableUnit findUnits(Unit unitReference, IQueryable<IInstallableUnit> queryable)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
        IQueryResult<IInstallableUnit> queryResult = findUnit(unitReference, queryable);

        if (queryResult.isEmpty()) {
            throw new TargetDefinitionResolutionException(
                    NLS.bind("Could not find \"{0}/{1}\" in the repositories of the current location",
                            unitReference.getId(), unitReference.getVersion()));
        }
        // if the repository contains the same iu/version twice, both are identical and it is OK to use either
        IInstallableUnit unitInstance = queryResult.iterator().next();
        return unitInstance;
    }

    private static IQueryResult<IInstallableUnit> findUnit(Unit unitReference, IQueryable<IInstallableUnit> units)
            throws TargetDefinitionSyntaxException {
        Version version = parseVersion(unitReference);

        // the createIUQuery treats 0.0.0 version as "any version", and all other versions as exact versions
        IQuery<IInstallableUnit> matchingIUQuery = QueryUtil.createIUQuery(unitReference.getId(), version);
        IQuery<IInstallableUnit> latestMatchingIUQuery = QueryUtil.createLatestQuery(matchingIUQuery);

        IQueryResult<IInstallableUnit> queryResult = units.query(latestMatchingIUQuery, new NullProgressMonitor());
        return queryResult;
    }

    private static Version parseVersion(Unit unitReference) throws TargetDefinitionSyntaxException {
        try {
            return Version.parseVersion(unitReference.getVersion());
        } catch (IllegalArgumentException e) {
            throw new TargetDefinitionSyntaxException(NLS.bind("Cannot parse version \"{0}\" of unit \"{1}\"",
                    unitReference.getVersion(), unitReference.getId()), e);
        }
    }

}
