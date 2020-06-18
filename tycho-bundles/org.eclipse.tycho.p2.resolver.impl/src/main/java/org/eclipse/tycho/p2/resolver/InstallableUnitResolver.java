/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - extract implementation from TargetDefinitionResolver and implement generic interface
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.util.resolution.AbstractResolutionStrategy;
import org.eclipse.tycho.p2.util.resolution.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.util.resolution.ProjectorResolutionStrategy;
import org.eclipse.tycho.p2.util.resolution.ResolutionDataImpl;
import org.eclipse.tycho.p2.util.resolution.ResolverException;
import org.eclipse.tycho.p2.util.resolution.SlicerResolutionStrategy;
import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;

public class InstallableUnitResolver implements Resolvable {

    private static final String SOURCE_IU_ID = "org.eclipse.tycho.internal.target.source.bundles";

    private List<IQueryable<IInstallableUnit>> availableUnitSources = new ArrayList<>();
    private Set<IInstallableUnit> rootIUs = new LinkedHashSet<>();

    private IncludeMode includeMode = null;
    private Boolean includeAllEnvironments = null;
    private Boolean includeSource = null;

    private IMetadataRepositoryManager metadataManager;

    private IRepositoryIdManager repositoryIdManager;

    private List<TargetEnvironment> environments;

    private ExecutionEnvironmentResolutionHints executionEnvironment;

    private IProgressMonitor monitor;

    private MavenLogger logger;

    public InstallableUnitResolver(IMetadataRepositoryManager metadataManager, IRepositoryIdManager repositoryIdManager,
            List<TargetEnvironment> environments, ExecutionEnvironmentResolutionHints executionEnvironment,
            MavenLogger logger) {
        this.metadataManager = metadataManager;
        this.repositoryIdManager = repositoryIdManager;
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.monitor = new DuplicateFilteringLoggingProgressMonitor(logger);
        this.logger = logger;
    }

    public void addLocation(InstallableUnitLocation iuLocationDefinition)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
        setIncludeMode(iuLocationDefinition.getIncludeMode());
        setIncludeAllEnvironments(iuLocationDefinition.includeAllEnvironments());
        setIncludeSource(iuLocationDefinition.includeSource());

        LoadedIULocation loadedLocation = new LoadedIULocation(iuLocationDefinition);
        rootIUs.addAll(loadedLocation.getRootIUs());

        availableUnitSources.addAll(loadedLocation.getAvailableUnits());
    }

    public List<IQueryable<IInstallableUnit>> getAvailableUnitSources() {
        return availableUnitSources;
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

    @Override
    public Collection<IInstallableUnit> resolve(IProgressMonitor monitor) throws ResolverException {
        if (addedLocationsHaveContent()) {
            //TODO this should be moved to a separate class
            ResolutionDataImpl data = new ResolutionDataImpl(executionEnvironment);
            data.setRootIUs(rootIUs);
            data.setAvailableIUsAndFilter(compoundQueriable(availableUnitSources));

            AbstractResolutionStrategy strategy = getResolutionStrategy();
            strategy.setData(data);
            Collection<IInstallableUnit> units = strategy.multiPlatformResolve(environments, monitor);
            if (includeSource && !units.isEmpty()) {
                addSourceBundleUnits(data, strategy, units);
            }
            return units;
        }
        return Collections.emptyList();
    }

    private boolean addedLocationsHaveContent() {
        return !availableUnitSources.isEmpty();
    }

    private AbstractResolutionStrategy getResolutionStrategy() throws TargetDefinitionResolutionException {
        switch (includeMode) {
        case PLANNER:
            return getPlannerResolutionStrategy();
        case SLICER:
            return getSlicerResolutionStrategy();
        default:
            throw new IllegalStateException();
        }
    }

    private AbstractResolutionStrategy getSlicerResolutionStrategy() {
        boolean ignoreFilters = includeAllEnvironments;
        return new SlicerResolutionStrategy(logger, ignoreFilters);
    }

    private AbstractResolutionStrategy getPlannerResolutionStrategy() throws TargetDefinitionResolutionException {
        if (includeAllEnvironments) {
            logger.warn(
                    "includeAllPlatforms='true' and includeMode='planner' are incompatible. ignore includeAllPlatforms flag");
        }
        return new ProjectorResolutionStrategy(logger);
    }

    private void addSourceBundleUnits(ResolutionDataImpl data, AbstractResolutionStrategy strategy,
            Collection<IInstallableUnit> units) throws ResolverException {
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
        // TODO also reconstruct strategy?
        data.setRootIUs(Collections.singleton(sourceIU));
        final TargetEnvironment nonFilteringEnvironment = new TargetEnvironment();
        Collection<IInstallableUnit> sourceUnits = strategy.resolve(nonFilteringEnvironment, monitor);
        sourceUnits.remove(sourceIU); // nobody wants to see our artificial IU
        units.addAll(sourceUnits); // TODO: remove duplicates?
    }

    private class LoadedIULocation {

        private InstallableUnitLocation locationDefinition;
        private List<IMetadataRepository> loadedRepositories;

        public LoadedIULocation(InstallableUnitLocation locationDefinition) throws TargetDefinitionResolutionException {
            this.locationDefinition = locationDefinition;

            loadedRepositories = new ArrayList<>();
            for (Repository repository : locationDefinition.getRepositories()) {
                repositoryIdManager.addMapping(repository.getId(), repository.getLocation());
                loadedRepositories.add(loadRepository(repository));
            }
        }

        private IMetadataRepository loadRepository(Repository repository) throws TargetDefinitionResolutionException {
            try {
                return metadataManager.loadRepository(repository.getLocation(), monitor);
            } catch (ProvisionException e) {
                throw new TargetDefinitionResolutionException(
                        "Failed to load p2 metadata repository from location " + repository.getLocation(), e);
            }
        }

        public Collection<? extends IQueryable<IInstallableUnit>> getAvailableUnits() {
            return loadedRepositories;
        }

        public Collection<? extends IInstallableUnit> getRootIUs()
                throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
            List<IInstallableUnit> result = new ArrayList<>();
            for (Unit unitReference : locationDefinition.getUnits()) {
                result.add(findUnitInThisLocation(unitReference));
            }
            return result;
        }

        private IInstallableUnit findUnitInThisLocation(Unit unitReference)
                throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
            IQueryResult<IInstallableUnit> queryResult = findUnit(unitReference, compoundQueriable(loadedRepositories));

            if (queryResult.isEmpty()) {
                throw new TargetDefinitionResolutionException(
                        NLS.bind("Could not find \"{0}/{1}\" in the repositories of the current location",
                                unitReference.getId(), unitReference.getVersion()));
            }
            // if the repository contains the same iu/version twice, both are identical and it is OK to use either
            IInstallableUnit unitInstance = queryResult.iterator().next();
            return unitInstance;
        }

        private IQueryResult<IInstallableUnit> findUnit(Unit unitReference, IQueryable<IInstallableUnit> units)
                throws TargetDefinitionSyntaxException {
            Version version = parseVersion(unitReference);

            // the createIUQuery treats 0.0.0 version as "any version", and all other versions as exact versions
            IQuery<IInstallableUnit> matchingIUQuery = QueryUtil.createIUQuery(unitReference.getId(), version);
            IQuery<IInstallableUnit> latestMatchingIUQuery = QueryUtil.createLatestQuery(matchingIUQuery);

            IQueryResult<IInstallableUnit> queryResult = units.query(latestMatchingIUQuery, new NullProgressMonitor());
            return queryResult;
        }

        private Version parseVersion(Unit unitReference) throws TargetDefinitionSyntaxException {
            try {
                return Version.parseVersion(unitReference.getVersion());
            } catch (IllegalArgumentException e) {
                throw new TargetDefinitionSyntaxException(NLS.bind("Cannot parse version \"{0}\" of unit \"{1}\"",
                        unitReference.getVersion(), unitReference.getId()), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static CompoundQueryable<IInstallableUnit> compoundQueriable(
            List<? extends IQueryable<IInstallableUnit>> queryable) {
        return new CompoundQueryable<>(queryable.toArray(new IQueryable[queryable.size()]));
    }

}
