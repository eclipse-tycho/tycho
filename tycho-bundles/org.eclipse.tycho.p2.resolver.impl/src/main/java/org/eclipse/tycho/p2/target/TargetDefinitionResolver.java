/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.CompoundQueryable;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.core.ee.shared.BuildFailureException;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
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

/**
 * Class which performs target definition resolution. This class is used by the
 * {@link TargetDefinitionResolverService} instance.
 * 
 * @see TargetDefinitionResolverService
 */
public final class TargetDefinitionResolver {

    private IMetadataRepositoryManager metadataManager;
    private IRepositoryIdManager repositoryIdManager;

    private final MavenLogger logger;

    private final List<TargetEnvironment> environments;

    private final ExecutionEnvironmentResolutionHints executionEnvironment;

    private final IProgressMonitor monitor;

    public TargetDefinitionResolver(List<TargetEnvironment> environments,
            ExecutionEnvironmentResolutionHints executionEnvironment, IProvisioningAgent agent, MavenLogger logger) {
        this.environments = environments;
        this.executionEnvironment = executionEnvironment;
        this.logger = logger;
        this.monitor = new DuplicateFilteringLoggingProgressMonitor(logger); // entails that this class is not thread-safe
        this.metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
        this.repositoryIdManager = (IRepositoryIdManager) agent.getService(IRepositoryIdManager.SERVICE_NAME);
    }

    public TargetDefinitionContent resolveContent(TargetDefinition definition) {
        try {
            return resolveContentWithExceptions(definition);
        } catch (TargetDefinitionSyntaxException e) {
            throw new BuildFailureException("Invalid syntax in target definition " + definition.getOrigin() + ": "
                    + e.getMessage(), e);
        } catch (TargetDefinitionResolutionException e) {
            throw new BuildFailureException("Failed to resolve target definition " + definition.getOrigin() + ": "
                    + e.getMessage(), e);
        } catch (ResolverException e) {
            throw new BuildFailureException("Failed to resolve target definition " + definition.getOrigin()
                    + ". See log for details.", e);
        }
    }

    TargetDefinitionContent resolveContentWithExceptions(TargetDefinition definition)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException, ResolverException {

        List<URI> artifactRepositories = new ArrayList<URI>();
        ResolverRun resolverRun = new ResolverRun();

        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                resolverRun.addLocation((InstallableUnitLocation) locationDefinition);

                for (Repository repository : ((InstallableUnitLocation) locationDefinition).getRepositories()) {
                    artifactRepositories.add(repository.getLocation());
                }
            } else {
                logger.warn("Target location type '" + locationDefinition.getTypeDescription() + "' is not supported");
            }
        }

        if (definition.hasIncludedBundles()) {
            // the bundle selection list is currently not taken into account (see bug 373776)
            logger.warn("De-selecting bundles in a target definition file is not supported. See http://wiki.eclipse.org/Tycho_Messages_Explained#Target_File_Include_Bundles for alternatives.");
        }

        return new TargetDefinitionContent(resolverRun.resolve(), artifactRepositories);
    }

    private class ResolverRun {

        private List<IQueryable<IInstallableUnit>> availableUnitSources = new ArrayList<IQueryable<IInstallableUnit>>();
        private Set<IInstallableUnit> rootIUs = new LinkedHashSet<IInstallableUnit>();

        private IncludeMode includeMode = null;
        private Boolean includeAllEnvironments = null;

        public void addLocation(InstallableUnitLocation iuLocationDefinition) throws TargetDefinitionSyntaxException,
                TargetDefinitionResolutionException {
            setIncludeMode(iuLocationDefinition.getIncludeMode());
            setIncludeAllEnvironments(iuLocationDefinition.includeAllEnvironments());

            LoadedIULocation loadedLocation = new LoadedIULocation(iuLocationDefinition);
            rootIUs.addAll(loadedLocation.getRootIUs());

            availableUnitSources.addAll(loadedLocation.getAvailableUnits());
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

        public Collection<IInstallableUnit> resolve() throws TargetDefinitionResolutionException, ResolverException {
            if (!addedLocationsHaveContent()) {
                return Collections.emptySet();
            }

            ResolutionDataImpl data = new ResolutionDataImpl(executionEnvironment);
            data.setRootIUs(rootIUs);
            data.setAvailableIUsAndFilter(compoundQueriable(availableUnitSources));

            AbstractResolutionStrategy strategy = getResolutionStrategy();
            strategy.setData(data);

            return strategy.multiPlatformResolve(environments, monitor);
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
                throw new TargetDefinitionResolutionException(
                        "includeAllPlatforms='true' and includeMode='planner' are incompatible.");
            }
            return new ProjectorResolutionStrategy(logger);
        }

    }

    private class LoadedIULocation {

        private InstallableUnitLocation locationDefinition;
        private List<IMetadataRepository> loadedRepositories;

        public LoadedIULocation(InstallableUnitLocation locationDefinition) throws TargetDefinitionResolutionException {
            this.locationDefinition = locationDefinition;

            loadedRepositories = new ArrayList<IMetadataRepository>();
            for (Repository repository : locationDefinition.getRepositories()) {
                repositoryIdManager.addMapping(repository.getId(), repository.getLocation());
                loadedRepositories.add(loadRepository(repository));
            }
        }

        private IMetadataRepository loadRepository(Repository repository) throws TargetDefinitionResolutionException {
            try {
                return metadataManager.loadRepository(repository.getLocation(), monitor);
            } catch (ProvisionException e) {
                throw new TargetDefinitionResolutionException("Failed to load p2 metadata repository from location "
                        + repository.getLocation(), e);
            }
        }

        public Collection<? extends IQueryable<IInstallableUnit>> getAvailableUnits() {
            return loadedRepositories;
        }

        public Collection<? extends IInstallableUnit> getRootIUs() throws TargetDefinitionSyntaxException,
                TargetDefinitionResolutionException {
            List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
            for (Unit unitReference : locationDefinition.getUnits()) {
                result.add(findUnitInThisLocation(unitReference));
            }
            return result;
        }

        private IInstallableUnit findUnitInThisLocation(Unit unitReference) throws TargetDefinitionSyntaxException,
                TargetDefinitionResolutionException {
            IQueryResult<IInstallableUnit> queryResult = findUnit(unitReference, compoundQueriable(loadedRepositories));

            if (queryResult.isEmpty()) {
                throw new TargetDefinitionResolutionException(NLS.bind(
                        "Could not find \"{0}/{1}\" in the repositories of the current location",
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
    static CompoundQueryable<IInstallableUnit> compoundQueriable(List<? extends IQueryable<IInstallableUnit>> queryable) {
        return new CompoundQueryable<IInstallableUnit>(queryable.toArray(new IQueryable[queryable.size()]));
    }
}
