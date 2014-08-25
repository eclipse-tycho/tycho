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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
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
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.facade.TargetEnvironment;
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
import org.eclipse.tycho.p2.util.resolution.SlicerResolutionStrategy;
import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;
import org.eclipse.tycho.repository.util.StatusTool;

/**
 * Class which performs target definition resolution. This class is used by the
 * {@link TargetDefinitionResolverService} instance.
 * 
 * @see TargetDefinitionResolverService
 */
public final class TargetDefinitionResolver {

    public enum IncludeSources {
        HONOR, IGNORE, FORCE;

        /**
         * parses a Boolean representation of the preference
         * <ul>
         * <li>null <=> HONOR</li>
         * <li>true <=> FORCE</li>
         * <li>false <=> IGNORE</li>
         * </ul>
         */
        public static IncludeSources fromBoolean(Boolean includeSource) {
            if (includeSource == null) {
                return HONOR;
            } else if (includeSource == Boolean.TRUE) {
                return FORCE;
            } else if (includeSource == Boolean.FALSE) {
                return IGNORE;
            }
            return HONOR;
        }
    }

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
            throw new RuntimeException("Invalid syntax in target definition " + definition.getOrigin() + ": "
                    + e.getMessage(), e);
        } catch (TargetDefinitionResolutionException e) {
            throw new RuntimeException("Failed to resolve target definition " + definition.getOrigin(), e);
        }
    }

    public TargetDefinitionContent resolveContent(TargetDefinition definition, IncludeSources includeSourcesMode) {
        try {
            return resolveContentWithExceptions(definition, includeSourcesMode);
        } catch (TargetDefinitionSyntaxException e) {
            throw new RuntimeException("Invalid syntax in target definition " + definition.getOrigin() + ": "
                    + e.getMessage(), e);
        } catch (TargetDefinitionResolutionException e) {
            throw new RuntimeException("Failed to resolve target definition " + definition.getOrigin(), e);
        }
    }

    TargetDefinitionContent resolveContentWithExceptions(TargetDefinition definition, IncludeSources includeSourcesMode)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {

        List<URI> artifactRepositories = new ArrayList<URI>();
        ResolverRun resolverRun = new ResolverRun();

        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                InstallableUnitLocation iusLocation = (InstallableUnitLocation) locationDefinition;
                boolean includeSources = iusLocation.includeSource();
                if (includeSourcesMode == IncludeSources.IGNORE) {
                    includeSources = false;
                } else if (includeSourcesMode == IncludeSources.FORCE) {
                    includeSources = true;
                }
                resolverRun.addLocation((InstallableUnitLocation) locationDefinition, includeSources);

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

    TargetDefinitionContent resolveContentWithExceptions(TargetDefinition definition)
            throws TargetDefinitionSyntaxException, TargetDefinitionResolutionException {
        return resolveContentWithExceptions(definition, IncludeSources.HONOR);
    }

    private class ResolverRun {

        private List<IQueryable<IInstallableUnit>> availableUnitSources = new ArrayList<IQueryable<IInstallableUnit>>();
        private Set<IInstallableUnit> rootIUs = new LinkedHashSet<IInstallableUnit>();

        private IncludeMode includeMode = null;
        private Boolean includeAllEnvironments = null;
        private Boolean includeSource = null;

        public void addLocation(InstallableUnitLocation iuLocationDefinition, boolean includeSources)
                throws TargetDefinitionSyntaxException {
            setIncludeMode(iuLocationDefinition.getIncludeMode());
            setIncludeAllEnvironments(iuLocationDefinition.includeAllEnvironments());
            setIncludeSource(includeSources);

            LoadedIULocation loadedLocation = new LoadedIULocation(iuLocationDefinition);
            rootIUs.addAll(loadedLocation.getRootIUs());

            availableUnitSources.addAll(loadedLocation.getAvailableUnits());
        }

        private void setIncludeMode(IncludeMode newValue) {
            if (includeMode != newValue) {
                if (includeMode != null) {
                    throw new TargetDefinitionResolutionException("Include mode must be the same for all locations");
                }
                includeMode = newValue;
            }
        }

        private void setIncludeAllEnvironments(Boolean newValue) {
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

        public Collection<IInstallableUnit> resolve() {
            if (!addedLocationsHaveContent()) {
                return Collections.emptySet();
            }

            ResolutionDataImpl data = new ResolutionDataImpl(executionEnvironment);
            data.setRootIUs(rootIUs);
            data.setAvailableIUsAndFilter(compoundQueriable(availableUnitSources));

            AbstractResolutionStrategy strategy = getResolutionStrategy();
            strategy.setData(data);
            Collection<IInstallableUnit> units = strategy.multiPlatformResolve(environments, monitor);
            if (includeSource) { // TODO do we have to check for units.isEmpty() here?
                ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
                for (IInstallableUnit unit : units) {
                    // TODO the PDE additionally checks, if the IU is a bundle (satisfies a requirement on namespace
                    // "org.eclipse.equinox.p2.eclipse.type" with value "bundle"), should we do the same here?
                    requirements
                            .add(MetadataFactory.createRequirement("osgi.bundle", unit.getId() + ".source",
                                    new VersionRange(unit.getVersion(), true, unit.getVersion(), true), null, true,
                                    false, true));
                }
                InstallableUnit sourceIU = new InstallableUnit();
                sourceIU.setRequiredCapabilities(requirements.toArray(new IRequirement[requirements.size()]));
                // TODO also reconstruct strategy?
                data.setRootIUs(Collections.singleton((IInstallableUnit) sourceIU));
                Collection<IInstallableUnit> sourceUnits = strategy.resolve(new TargetEnvironment(), monitor);
                sourceUnits.remove(sourceIU); // nobody wants to see our artificial IU
                units.addAll(sourceUnits); // TODO: remove duplicates?
            }

            return units;
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
            return new SlicerResolutionStrategy(logger, ignoreFilters) {

                @Override
                protected RuntimeException newResolutionException(IStatus status) {
                    return TargetDefinitionResolver.newResolutionException(status);
                }
            };
        }

        private AbstractResolutionStrategy getPlannerResolutionStrategy() throws TargetDefinitionResolutionException {
            if (includeAllEnvironments) {
                throw new TargetDefinitionResolutionException(
                        "includeAllPlatforms='true' and includeMode='planner' are incompatible.");
            }
            return new ProjectorResolutionStrategy(logger) {
                @Override
                protected RuntimeException newResolutionException(IStatus status) {
                    return TargetDefinitionResolver.newResolutionException(status);
                }
            };
        }

    }

    private class LoadedIULocation {

        private InstallableUnitLocation locationDefinition;
        private List<IMetadataRepository> loadedRepositories;

        public LoadedIULocation(InstallableUnitLocation locationDefinition) {
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

        public Collection<? extends IInstallableUnit> getRootIUs() throws TargetDefinitionSyntaxException {
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

    static RuntimeException newResolutionException(IStatus status) {
        return new TargetDefinitionResolutionException(StatusTool.collectProblems(status), new CoreException(status));
    }

    @SuppressWarnings("unchecked")
    static CompoundQueryable<IInstallableUnit> compoundQueriable(List<? extends IQueryable<IInstallableUnit>> queryable) {
        return new CompoundQueryable<IInstallableUnit>(queryable.toArray(new IQueryable[queryable.size()]));
    }
}
