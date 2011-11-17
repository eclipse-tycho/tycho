/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.util.StatusTool;

/**
 * TODO respect target execution environment profile. Current implementation assumes current JRE and
 * will select wrong installable units for restricted target profiles like OSGi/Minimum-1.0
 */
public class TargetDefinitionResolver {
    private static final IInstallableUnit[] EMPTY_IU_ARRAY = new IInstallableUnit[0];

    private IMetadataRepositoryManager metadataManager;

    private final MavenLogger logger;

    private final List<Map<String, String>> environments;

    public TargetDefinitionResolver(List<Map<String, String>> environments, IProvisioningAgent agent, MavenLogger logger) {
        this.environments = environments;
        this.logger = logger;
        this.metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
    }

    public TargetPlatformContent resolveContent(TargetDefinition definition) throws TargetDefinitionSyntaxException,
            TargetDefinitionResolutionException {

        List<URI> artifactRepositories = new ArrayList<URI>();
        IUResolver resolverRun = new IUResolver();

        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                InstallableUnitLocation iuLocationDefinition = (InstallableUnitLocation) locationDefinition;
                resolverRun.addLocation(iuLocationDefinition);

                for (Repository repository : iuLocationDefinition.getRepositories()) {
                    artifactRepositories.add(repository.getLocation());
                }
            } else {
                logger.warn(NLS.bind("Target location type: {0} is not supported",
                        locationDefinition.getTypeDescription()));
            }
        }
        return new ResolvedDefinition(resolverRun.execute(), artifactRepositories);
    }

    private class IUResolver {

        private List<LoadedLocation> locations = new ArrayList<LoadedLocation>();
        List<IQueryable<IInstallableUnit>> availableUnits = new ArrayList<IQueryable<IInstallableUnit>>();
        List<IInstallableUnit> seedUnits = new ArrayList<IInstallableUnit>();

        private IncludeMode includeMode = null;
        private Boolean includeAllEnvironments = null;

        void addLocation(InstallableUnitLocation locationDefinition) {
            setIncludeMode(locationDefinition.getIncludeMode());
            setIncludeAllEnvironments(locationDefinition.includeAllEnvironments());

            LoadedLocation loadedLocation = new LoadedLocation(locationDefinition);
            locations.add(loadedLocation);

            availableUnits.add(loadedLocation.getAvailableUnits());
            seedUnits.addAll(loadedLocation.getSeedUnits());
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

        private void setIncludeMode(IncludeMode newValue) {
            if (includeMode != newValue) {
                if (includeMode != null) {
                    throw new TargetDefinitionResolutionException("Include mode must be the same for all locations");
                }
                includeMode = newValue;
            }
        }

        Collection<IInstallableUnit> execute() {
            if (locations.isEmpty()) {
                return Collections.emptyList();
            }

            final Collection<IInstallableUnit> resolvedUnits;
            switch (includeMode) {
            case PLANNER:
                resolvedUnits = resolveWithPlanner();
                break;
            case SLICER:
                resolvedUnits = resolveWithSlicer();
                break;
            default:
                throw new IllegalStateException();
            }
            return resolvedUnits;
        }

        private Collection<IInstallableUnit> resolveWithSlicer() {
            IQueryable<IInstallableUnit> availableUnitsQueryable = QueryUtil.compoundQueryable(availableUnits);

            Set<IInstallableUnit> result = new HashSet<IInstallableUnit>();
            if (includeAllEnvironments) {
                Map<String, String> selectionContext = Collections.emptyMap();
                result.addAll(sliceForPlatform(seedUnits, availableUnitsQueryable, selectionContext));
            } else {
                for (Map<String, String> environment : environments) {
                    Map<String, String> selectionContext = addFeatureJarFilter(environment);
                    result.addAll(sliceForPlatform(seedUnits, availableUnitsQueryable, selectionContext));
                }
            }
            return result;
        }

        @SuppressWarnings("restriction")
        private Set<IInstallableUnit> sliceForPlatform(List<IInstallableUnit> seedUnits,
                IQueryable<IInstallableUnit> availableUnits, Map<String, String> selectionContext) {
            NullProgressMonitor monitor = new NullProgressMonitor();

            boolean evalFilterTo = selectionContext.isEmpty();
            Slicer slicer = new PermissiveSlicer(availableUnits, selectionContext, true, false, evalFilterTo, true,
                    false);

            IQueryable<IInstallableUnit> slice = slicer.slice(seedUnits.toArray(EMPTY_IU_ARRAY), monitor);

            MultiStatus slicerStatus = slicer.getStatus();
            if (slicerStatus.matches(IStatus.WARNING | IStatus.ERROR | IStatus.CANCEL)) {
                throw new TargetDefinitionResolutionException(StatusTool.collectProblems(slicerStatus),
                        StatusTool.findException(slicer.getStatus()));
            }

            return slice.query(QueryUtil.ALL_UNITS, monitor).toSet();
        }

        private Map<String, String> addFeatureJarFilter(Map<String, String> environment) {
            final Map<String, String> selectionContext;
            selectionContext = new HashMap<String, String>(environment);
            selectionContext.put("org.eclipse.update.install.features", "true");
            return selectionContext;
        }

        private Collection<IInstallableUnit> resolveWithPlanner() {
            IQueryable<IInstallableUnit> availableUnitsQueryable = QueryUtil.compoundQueryable(availableUnits);
            Collection<IInstallableUnit> result = new HashSet<IInstallableUnit>();

            for (Map<String, String> environment : environments) {
                Map<String, String> selectionContext = addFeatureJarFilter(environment);
                Collection<IInstallableUnit> resolvedUnits = planForPlatform(seedUnits, availableUnitsQueryable,
                        selectionContext);
                result.addAll(resolvedUnits);
            }

            return result;
        }

        // TODO share this code with ProjectorResolutionStrategy
        @SuppressWarnings("restriction")
        private Collection<IInstallableUnit> planForPlatform(List<IInstallableUnit> seedUnits,
                IQueryable<IInstallableUnit> availableUnits, Map<String, String> selectionContext) {
            IProgressMonitor monitor = new NullProgressMonitor();
            Slicer slicer = new Slicer(availableUnits, selectionContext, false);
            IQueryable<IInstallableUnit> slice = slicer.slice(seedUnits.toArray(EMPTY_IU_ARRAY), monitor);

            if (slice == null) {
                MultiStatus slicerStatus = slicer.getStatus();
                throw new TargetDefinitionResolutionException(StatusTool.collectProblems(slicerStatus),
                        StatusTool.findException(slicer.getStatus()));
            }

            Projector projector = new Projector(slice, selectionContext, new HashSet<IInstallableUnit>(), false);
            projector.encode(createMetaIU(seedUnits), EMPTY_IU_ARRAY /* alreadyExistingRoots */, new QueryableArray(
                    EMPTY_IU_ARRAY) /* installed IUs */, seedUnits /* newRoots */, monitor);
            IStatus s = projector.invokeSolver(monitor);

            if (s.getSeverity() == IStatus.ERROR) {
                Set<Explanation> explanation = projector.getExplanation(monitor);
                logger.error("Cannot resolve target definition file:");
                // explanation is sorted reasonably
                for (Explanation explanationLine : explanation) {
                    logger.error("  " + explanationLine.toString());
                }
                logger.error("");
                throw new TargetDefinitionResolutionException(explanation.toString());
            }
            Collection<IInstallableUnit> resolvedUnits = projector.extractSolution();
            return resolvedUnits;
        }

        private IInstallableUnit createMetaIU(Collection<IInstallableUnit> rootIUs) {
            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            String time = Long.toString(System.currentTimeMillis());
            iud.setId(time);
            iud.setVersion(Version.createOSGi(0, 0, 0, time));

            ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
            for (IInstallableUnit iu : rootIUs) {
                VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
                requirements
                        .add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range,
                                iu.getFilter(), 1 /* min */, iu.isSingleton() ? 1 : Integer.MAX_VALUE /* max */, true /* greedy */));
            }

            iud.setRequirements(requirements.toArray(new IRequirement[requirements.size()]));
            return MetadataFactory.createInstallableUnit(iud);
        }

    }

    private class LoadedLocation {
        private final InstallableUnitLocation location;
        private IQueryable<IInstallableUnit> repositoryUnits;

        LoadedLocation(InstallableUnitLocation location) {
            this.location = location;
            List<? extends Repository> repositories = location.getRepositories();
            List<IQueryable<IInstallableUnit>> loadedRepositories = new ArrayList<IQueryable<IInstallableUnit>>();
            for (Repository repository : repositories) {
                IMetadataRepository loadedRepository = loadRepository(repository);
                loadedRepositories.add(loadedRepository);
            }
            if (loadedRepositories.size() == 1) {
                repositoryUnits = loadedRepositories.get(0);
            } else {
                repositoryUnits = QueryUtil.compoundQueryable(loadedRepositories);
            }

        }

        private IMetadataRepository loadRepository(Repository repository) {
            try {
                return metadataManager.loadRepository(repository.getLocation(), null);
            } catch (ProvisionException e) {
                throw new TargetDefinitionResolutionException("Failed to load metadata repository from location "
                        + repository.getLocation(), e);
            }
        }

        IQueryable<IInstallableUnit> getAvailableUnits() {
            return repositoryUnits;
        }

        Set<IInstallableUnit> getSeedUnits() {
            Set<IInstallableUnit> result = new HashSet<IInstallableUnit>();
            for (Unit unit : location.getUnits()) {
                result.add(getUnitInstance(unit));
            }
            return result;
        }

        private IInstallableUnit getUnitInstance(Unit unitReference) {
            IQueryResult<IInstallableUnit> queryResult = searchUnitInThisLocation(unitReference);

            if (queryResult.isEmpty()) {
                throw new TargetDefinitionResolutionException(NLS.bind(
                        "Could not find \"{0}/{1}\" in the repositories of the current location",
                        unitReference.getId(), unitReference.getVersion()));
            }
            // if the repository contains the same iu/version twice, both are identical and
            // it is OK to use either 
            IInstallableUnit unitInstance = queryResult.iterator().next();
            return unitInstance;
        }

        private IQueryResult<IInstallableUnit> searchUnitInThisLocation(Unit unitReference) {
            Version version = parseVersion(unitReference);

            // the createIUQuery treats 0.0.0 version as "any version", and all other versions as exact versions
            IQuery<IInstallableUnit> matchingIUQuery = QueryUtil.createIUQuery(unitReference.getId(), version);
            IQuery<IInstallableUnit> latestMatchingIUQuery = QueryUtil.createLatestQuery(matchingIUQuery);

            IQueryResult<IInstallableUnit> queryResult = repositoryUnits.query(latestMatchingIUQuery, null);
            return queryResult;
        }

        private Version parseVersion(Unit unitReference) {
            try {
                return Version.parseVersion(unitReference.getVersion());
            } catch (IllegalArgumentException e) {
                throw new TargetDefinitionSyntaxException(NLS.bind("Cannot parse version \"{0}\" of unit \"{1}\"",
                        unitReference.getVersion(), unitReference.getId()), e);
            }
        }
    }

    private static class ResolvedDefinition implements TargetPlatformContent {

        private Collection<? extends IInstallableUnit> units;
        private Collection<URI> artifactRepositories;

        public ResolvedDefinition(Collection<? extends IInstallableUnit> units, Collection<URI> artifactRepositories) {
            this.units = units;
            this.artifactRepositories = artifactRepositories;
        }

        public Collection<? extends IInstallableUnit> getUnits() {
            return units;
        }

        public Collection<URI> getArtifactRepositoryLocations() {
            return artifactRepositories;
        }
    }
}
