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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.resolver.AbstractResolutionStrategy;
import org.eclipse.tycho.p2.impl.resolver.ProjectorResolutionStrategy;
import org.eclipse.tycho.p2.impl.resolver.SlicerResolutionStrategy;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.p2.util.StatusTool;

public class TargetDefinitionResolver {

    private IMetadataRepositoryManager metadataManager;

    private final MavenLogger logger;

    private final List<Map<String, String>> environments;

    private final JREInstallableUnits jreIUs;

    private final IProgressMonitor monitor = new NullProgressMonitor();

    public TargetDefinitionResolver(List<Map<String, String>> environments, JREInstallableUnits jreIUs,
            IProvisioningAgent agent, MavenLogger logger) {
        this.environments = environments;
        this.jreIUs = jreIUs;
        this.logger = logger;
        this.metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
    }

    public TargetPlatformContent resolveContent(TargetDefinition definition) throws TargetDefinitionSyntaxException,
            TargetDefinitionResolutionException {

        List<URI> artifactRepositories = new ArrayList<URI>();

        Set<IInstallableUnit> availableUnits = new LinkedHashSet<IInstallableUnit>();

        Set<IInstallableUnit> rootIUs = new LinkedHashSet<IInstallableUnit>();

        IncludeMode includeMode = null;
        Boolean includeAllEnvironments = null;

        for (Location locationDefinition : definition.getLocations()) {
            if (locationDefinition instanceof InstallableUnitLocation) {
                InstallableUnitLocation iuLocationDefinition = (InstallableUnitLocation) locationDefinition;

                if (includeMode != null && includeMode != iuLocationDefinition.getIncludeMode()) {
                    throw new TargetDefinitionResolutionException("Include mode must be the same for all locations");
                }
                includeMode = iuLocationDefinition.getIncludeMode();

                if (includeAllEnvironments != null
                        && includeAllEnvironments.booleanValue() != iuLocationDefinition.includeAllEnvironments()) {
                    throw new TargetDefinitionResolutionException(
                            "The attribute 'includeAllPlatforms' must be the same for all locations");
                }
                includeAllEnvironments = iuLocationDefinition.includeAllEnvironments();

                List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();
                for (Repository repository : iuLocationDefinition.getRepositories()) {
                    artifactRepositories.add(repository.getLocation());
                    metadataRepositories.add(loadRepository(repository));
                }

                IQueryable<IInstallableUnit> locationUnits = new CompoundQueryable<IInstallableUnit>(
                        metadataRepositories.toArray(new IMetadataRepository[metadataRepositories.size()]));

                for (Unit unit : iuLocationDefinition.getUnits()) {
                    rootIUs.add(getUnitInstance(locationUnits, unit));
                }

                Iterator<IInstallableUnit> iterator = locationUnits.query(QueryUtil.ALL_UNITS, monitor).iterator();
                while (iterator.hasNext()) {
                    IInstallableUnit unit = iterator.next();
                    if (!jreIUs.isJREUI(unit)) {
                        availableUnits.add(unit);
                    }
                }
            } else {
                logger.warn(NLS.bind("Target location type: {0} is not supported",
                        locationDefinition.getTypeDescription()));
            }
        }

        Collection<IInstallableUnit> units;
        if (!availableUnits.isEmpty()) {
            AbstractResolutionStrategy strategy = getResolutionStrategy(includeMode, includeAllEnvironments);

            strategy.setRootInstallableUnits(rootIUs);
            strategy.setAvailableInstallableUnits(availableUnits);
            strategy.setJREUIs(jreIUs.getJREIUs());
            units = strategy.resolve(environments, monitor);
        } else {
            units = Collections.emptySet();
        }

        if (definition.hasIncludedBundles()) {
            // the bundle selection list is currently not taken into account (see bug 373776)
            logger.warn("De-selecting bundles in a target definition file is not supported. See http://wiki.eclipse.org/Tycho_Messages_Explained#Target_File_Include_Bundles for alternatives.");
        }

        return new ResolvedDefinition(units, artifactRepositories);
    }

    private AbstractResolutionStrategy getResolutionStrategy(IncludeMode includeMode, Boolean includeAllEnvironments) {
        switch (includeMode) {
        case PLANNER:
            return getPlannerResolutionStrategy(includeAllEnvironments);
        case SLICER:
            return getSlicerResolutionStrategy(includeAllEnvironments);
        default:
            throw new IllegalStateException();
        }
    }

    private AbstractResolutionStrategy getSlicerResolutionStrategy(final boolean ignoreFilters) {
        return new SlicerResolutionStrategy(logger) {
            @Override
            public Collection<IInstallableUnit> resolve(List<Map<String, String>> allproperties,
                    IProgressMonitor monitor) {
                if (ignoreFilters) {
                    return resolve(Collections.<String, String> emptyMap(), monitor);
                }
                return super.resolve(allproperties, monitor);
            }

            protected boolean ignoreFilters() {
                return ignoreFilters;
            };

            protected RuntimeException newResolutionException(IStatus status) {
                return TargetDefinitionResolver.this.newResolutionException(status);
            };
        };
    }

    private AbstractResolutionStrategy getPlannerResolutionStrategy(boolean includeAllEnvironments) {
        if (includeAllEnvironments) {
            throw new TargetDefinitionResolutionException(
                    "includeAllPlatforms='true' and includeMode='planner' are incompatible.");
        }
        return new ProjectorResolutionStrategy(logger) {
            protected RuntimeException newResolutionException(IStatus status) {
                return TargetDefinitionResolver.this.newResolutionException(status);
            };
        };
    }

    private IMetadataRepository loadRepository(Repository repository) {
        try {
            return metadataManager.loadRepository(repository.getLocation(), monitor);
        } catch (ProvisionException e) {
            throw new TargetDefinitionResolutionException("Failed to load metadata repository from location "
                    + repository.getLocation(), e);
        }
    }

    private IInstallableUnit getUnitInstance(IQueryable<IInstallableUnit> units, Unit unitReference) {
        IQueryResult<IInstallableUnit> queryResult = searchUnitInThisLocation(units, unitReference);

        if (queryResult.isEmpty()) {
            throw new TargetDefinitionResolutionException(NLS.bind(
                    "Could not find \"{0}/{1}\" in the repositories of the current location", unitReference.getId(),
                    unitReference.getVersion()));
        }
        // if the repository contains the same iu/version twice, both are identical and
        // it is OK to use either 
        IInstallableUnit unitInstance = queryResult.iterator().next();
        return unitInstance;
    }

    private IQueryResult<IInstallableUnit> searchUnitInThisLocation(IQueryable<IInstallableUnit> units,
            Unit unitReference) {
        Version version = parseVersion(unitReference);

        // the createIUQuery treats 0.0.0 version as "any version", and all other versions as exact versions
        IQuery<IInstallableUnit> matchingIUQuery = QueryUtil.createIUQuery(unitReference.getId(), version);
        IQuery<IInstallableUnit> latestMatchingIUQuery = QueryUtil.createLatestQuery(matchingIUQuery);

        IQueryResult<IInstallableUnit> queryResult = units.query(latestMatchingIUQuery, monitor);
        return queryResult;
    }

    /* package */RuntimeException newResolutionException(IStatus status) {
        return new TargetDefinitionResolutionException(StatusTool.collectProblems(status), new CoreException(status));
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
