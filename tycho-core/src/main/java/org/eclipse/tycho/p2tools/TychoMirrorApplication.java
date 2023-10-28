/*******************************************************************************
 * Copyright (c) 2010, 2023 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Hannes Wellmann - Assemble repository for all environments in one pass
 *     Hannes Wellmann - Implement user-defined filtering and filtering based on relevance for automatically added repo-references
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.RepositoryReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TychoMirrorApplication extends org.eclipse.tycho.p2tools.copiedfromp2.MirrorApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TychoMirrorApplication.class);
    private static final String SOURCE_SUFFIX = ".source";
    private static final String FEATURE_GROUP = ".feature.group";
    private final DestinationRepositoryDescriptor destination;
    private boolean includeAllSource;
    private boolean includeRequiredBundles;
    private boolean includeRequiredFeatures;
    private boolean filterProvided;
    private boolean addOnlyProvidingRepoReferences;
    private TargetPlatform targetPlatform;

    public TychoMirrorApplication(IProvisioningAgent agent, DestinationRepositoryDescriptor destination) {
        super(agent);
        this.destination = destination;
        this.removeAddedRepositories = false;
    }

    @Override
    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        IArtifactRepository result = super.initializeDestination(toInit, mgr);
        // simple.SimpleArtifactRepository.PUBLISH_PACK_FILES_AS_SIBLINGS is not public
        result.setProperty("publishPackFilesAsSiblings", "true");
        destination.getExtraArtifactRepositoryProperties().forEach(result::setProperty);
        return result;
    }

    @Override
    protected Slicer createSlicer(SlicingOptions options) {
        List<Map<String, String>> filters = getContextFilters();
        List<IInstallableUnit> selectionContexts = filters.stream().map(InstallableUnit::contextIU).toList();
        boolean includeOptionalDependencies = options.includeOptionalDependencies();
        boolean onlyFilteredRequirements = options.followOnlyFilteredRequirements();
        boolean considerFilter = filters.stream().anyMatch(f -> f.size() > 1);
        boolean evalFilterTo = options.forceFilterTo();
        IMetadataRepository repository = getCompositeMetadataRepository();
        boolean considerOnlyStrictDependency = options.considerStrictDependencyOnly();
        return new PermissiveSlicer(repository, filters.get(0), includeOptionalDependencies,
                options.isEverythingGreedy(), evalFilterTo, considerOnlyStrictDependency, onlyFilteredRequirements) {
            @Override
            protected boolean isApplicable(IInstallableUnit iu, IRequirement req) {
                if ((includeRequiredBundles || includeRequiredFeatures) && QueryUtil.isGroup(iu)
                        && req instanceof IRequiredCapability capability
                        && IInstallableUnit.NAMESPACE_IU_ID.equals(capability.getNamespace())) {
                    boolean isFeature = capability.getName().endsWith(FEATURE_GROUP);
                    if ((isFeature && includeRequiredFeatures) || (!isFeature && includeRequiredBundles)) {
                        if (!includeOptionalDependencies && req.getMin() == 0) {
                            return false;
                        }
                        IMatchExpression<IInstallableUnit> filter = req.getFilter();
                        if (onlyFilteredRequirements && filter == null) {
                            return false;
                        }
                        return !considerFilter || filter == null || matchesSelectionContext(filter);
                    }
                }
                return isApplicable(req);
            }

            @Override
            protected boolean isApplicable(IRequirement req) {
                //Every filter in this method needs to continue except when the filter does not pass
                if (!includeOptionalDependencies && req.getMin() == 0) {
                    return false;
                }
                if (considerOnlyStrictDependency && !RequiredCapability.isStrictVersionRequirement(req.getMatches())) {
                    return false;
                }
                //deal with filters
                IMatchExpression<IInstallableUnit> filter = req.getFilter();
                if (considerFilter) {
                    if (onlyFilteredRequirements && filter == null) {
                        return false;
                    }
                    return filter == null || matchesSelectionContext(filter);
                }
                return filter == null ? !onlyFilteredRequirements : evalFilterTo;
            }

            @Override
            protected boolean isApplicable(IInstallableUnit iu) {
                if (considerFilter) {
                    IMatchExpression<IInstallableUnit> filter = iu.getFilter();
                    return filter == null || matchesSelectionContext(filter);
                }
                return iu.getFilter() == null || evalFilterTo;
            }

            private boolean matchesSelectionContext(IMatchExpression<IInstallableUnit> filter) {
                return selectionContexts.stream().anyMatch(filter::isMatch);
            }

            @Override
            public IQueryable<IInstallableUnit> slice(IInstallableUnit[] ius, IProgressMonitor monitor) {
                IQueryable<IInstallableUnit> slice = super.slice(ius, monitor);
                if (includeAllSource && targetPlatform != null) {
                    Set<IInstallableUnit> collected = slice.query(QueryUtil.ALL_UNITS, null).toSet();
                    Set<IInstallableUnit> result = new HashSet<>(collected);
                    var allUnits = targetPlatform.getMetadataRepository().query(QueryUtil.ALL_UNITS, null);
                    Map<String, List<IInstallableUnit>> sourceIus = stream(allUnits)
                            .filter(iu -> iu.getId().endsWith(SOURCE_SUFFIX))
                            .collect(groupingBy(IInstallableUnit::getId));
                    for (IInstallableUnit iu : collected) {
                        String id = iu.getId();
                        String sourceId = id.endsWith(FEATURE_GROUP)
                                ? id.substring(id.length() - FEATURE_GROUP.length()) + SOURCE_SUFFIX
                                : id + SOURCE_SUFFIX;
                        List<IInstallableUnit> sourceUnits = sourceIus.get(sourceId);
                        if (sourceUnits != null) {
                            sourceUnits.stream().filter(su -> su.getVersion().equals(iu.getVersion())) //
                                    .findFirst().ifPresent(result::add);
                        }
                    }
                    return new CollectionResult<>(result);
                }
                return slice;
            }
        };
    }

    @Override
    protected IMetadataRepository initializeDestination(RepositoryDescriptor toInit, IMetadataRepositoryManager mgr)
            throws ProvisionException {
        IMetadataRepository result = super.initializeDestination(toInit, mgr);
        var refs = Stream.of(destination.getRepositoryReferences(), destination.getFilterableRepositoryReferences())
                .flatMap(List::stream).flatMap(TychoMirrorApplication::toSpiRepositoryReferences).toList();
        result.addReferences(refs);
        return result;
    }

    private static Stream<org.eclipse.equinox.p2.repository.spi.RepositoryReference> toSpiRepositoryReferences(
            RepositoryReference rr) {
        return Stream.of(IRepository.TYPE_METADATA, IRepository.TYPE_ARTIFACT).map(type -> {
            URI location = getNormalizedLocation(rr);
            int options = rr.isEnable() ? IRepository.ENABLED : IRepository.NONE;
            return new org.eclipse.equinox.p2.repository.spi.RepositoryReference(location, rr.getName(), type, options);
        });
    }

    private static URI getNormalizedLocation(RepositoryReference r) {
        // P2 does the same before loading the repo and thus IRepository.getLocation() returns the normalized URL.
        // In order to avoid stripping of slashes from URI instances do it now before URIs are created.
        String location = r.getLocation();
        return URI.create(location.endsWith("/") ? location.substring(0, location.length() - 1) : location);
    }

    @Override
    protected void finalizeRepositories() {
        IMetadataRepository repository = getDestinationMetadataRepository();
        if (repository != null) {
            Collection<IRepositoryReference> references = repository.getReferences();
            if (!references.isEmpty()) {
                LOGGER.info("Adding references to the following repositories:");
                references.stream().map(r -> r.getLocation()).distinct().forEach(loc -> LOGGER.info("  {}", loc));
            }
        }
        super.finalizeRepositories();
    }

    @Override
    protected List<IArtifactKey> collectArtifactKeys(Collection<IInstallableUnit> ius, IProgressMonitor monitor)
            throws ProvisionException {
        List<IArtifactKey> keys = super.collectArtifactKeys(ius, monitor);
        if (isFilterProvidedItems()) {
            removeProvidedItems(keys, getArtifactRepositoryManager(), IRepository.TYPE_ARTIFACT, monitor);
        }
        return keys;
    }

    @Override
    protected Set<IInstallableUnit> collectUnits(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor)
            throws ProvisionException {
        Set<IInstallableUnit> units = super.collectUnits(slice, monitor);
        if (isFilterProvidedItems()) {
            Map<String, List<Version>> fullRepositoryContent = units.stream()
                    .collect(groupingBy(IInstallableUnit::getId, mapping(IInstallableUnit::getVersion, toList())));

            List<IRepository<IInstallableUnit>> metadataRepositories = removeProvidedItems(units,
                    getMetadataRepositoryManager(), IRepository.TYPE_METADATA, monitor);

            if (addOnlyProvidingRepoReferences) {
                Set<URI> removableReferences = destination.getFilterableRepositoryReferences().stream()
                        .map(TychoMirrorApplication::getNormalizedLocation).collect(Collectors.toSet());
                destination.getRepositoryReferences().stream().map(TychoMirrorApplication::getNormalizedLocation)
                        .forEach(removableReferences::remove); // keep reference if explicitly added to the repository
                if (!removableReferences.isEmpty()) {
                    // Assume that for all units that correspond to artifacts the metadata either has a co-located artifact repository or a references to to one that contains it.
                    removeNotProvidingReferences(fullRepositoryContent, metadataRepositories, removableReferences);
                }
            }
        }
        return units;
    }

    private boolean isFilterProvidedItems() {
        return filterProvided && !destinationMetadataRepository.getReferences().isEmpty();
    }

    private <T> List<IRepository<T>> removeProvidedItems(Collection<T> allElements, IRepositoryManager<T> repoManager,
            int repositoryType, IProgressMonitor monitor) throws ProvisionException {
        List<IRepository<T>> referencedRepositories = new ArrayList<>();
        for (IRepositoryReference reference : destinationMetadataRepository.getReferences()) {
            if (reference.getType() != repositoryType) {
                continue;
            }
            try {
                URI location = reference.getLocation();
                IRepository<T> repository = loadRepository(repoManager, location, monitor);
                referencedRepositories.add(repository);
            } catch (IllegalArgumentException e) {
                if (e.getCause() instanceof URISyntaxException uriException) {
                    throw new ProvisionException("Can't parse referenced URI!", uriException);
                } else {
                    throw e;
                }
            }
        }
        allElements.removeIf(e -> referencedRepositories.stream().anyMatch(repo -> contains(repo, e)));
        return referencedRepositories;
    }

    private void removeNotProvidingReferences(Map<String, List<Version>> fullRepositoryContent,
            List<IRepository<IInstallableUnit>> metadataRepositories, Set<URI> removableReferenceURIs) {
        Map<URI, Set<IInstallableUnit>> usedRepositoryItems = new HashMap<>();
        for (IRepository<IInstallableUnit> repo : metadataRepositories) {
            IQueryResult<IInstallableUnit> allUnits = repo.query(QueryUtil.ALL_UNITS, null);
            Set<IInstallableUnit> usedRepoContent = stream(allUnits)
                    .filter(a -> fullRepositoryContent.getOrDefault(a.getId(), List.of()).contains(a.getVersion()))
                    .collect(Collectors.toSet());
            usedRepositoryItems.put(repo.getLocation(), usedRepoContent);
        }
        // Remove filterable references that contribute nothing or whose relevant content is also provided by another repo
        usedRepositoryItems.entrySet().removeIf(repo -> {
            if (!removableReferenceURIs.contains(repo.getKey())) {
                return false;
            }
            Set<IInstallableUnit> usedContent = repo.getValue();
            return usedContent.isEmpty()
                    || usedRepositoryItems.entrySet().stream().filter(e -> e != repo).map(Entry::getValue)
                            .anyMatch(other -> other.size() >= usedContent.size() && other.containsAll(usedContent));
        });
        IMetadataRepository repository = getDestinationMetadataRepository();
        List<IRepositoryReference> discardedReferences = repository.getReferences().stream()
                .filter(rr -> !usedRepositoryItems.keySet().contains(rr.getLocation())).toList();
        removeRepositoryReferences(repository, discardedReferences);
    }

    //TODO: Just call IMetadataRepository.removeReferences once available: https://github.com/eclipse-equinox/p2/pull/338
    private static void removeRepositoryReferences(IMetadataRepository metadataRepository,
            Collection<? extends IRepositoryReference> references) {
        if (metadataRepository instanceof LocalMetadataRepository localRepo) {
            try {
                Field repositoriesField = LocalMetadataRepository.class.getDeclaredField("repositories");
                repositoriesField.trySetAccessible();
                Method save = LocalMetadataRepository.class.getDeclaredMethod("save");
                save.trySetAccessible();
                @SuppressWarnings("unchecked")
                Set<IRepositoryReference> repositories = (Set<IRepositoryReference>) repositoriesField.get(localRepo);
                repositories.removeAll(references);
                save.invoke(localRepo);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to clean-up references from assembled repository", e);
            }
        }
    }

    //TODO: just call IRepositoryManager.loadRepository() once available: https://github.com/eclipse-equinox/p2/pull/311
    @SuppressWarnings("unchecked")
    private static <T> IRepository<T> loadRepository(IRepositoryManager<T> mgr, URI location, IProgressMonitor monitor)
            throws ProvisionException {
        if (mgr instanceof IArtifactRepositoryManager artifactRepoManager) {
            return (IRepository<T>) artifactRepoManager.loadRepository(location, monitor);
        } else if (mgr instanceof IMetadataRepositoryManager metadataMangager) {
            return (IRepository<T>) metadataMangager.loadRepository(location, monitor);
        }
        throw new AssertionError("Unsupported IRepositoryManager type" + mgr.getClass());
    }

    //TODO: just call IRepositoryManager.contains() once available: https://github.com/eclipse-equinox/p2/pull/314
    private static <T> boolean contains(IRepository<T> repository, T element) {
        if (repository instanceof IArtifactRepository artifactRepository) {
            return artifactRepository.contains((IArtifactKey) element);
        } else if (repository instanceof IMetadataRepository metadataRepository) {
            return !metadataRepository.query(QueryUtil.createIUQuery((IInstallableUnit) element), null).isEmpty();
        }
        throw new AssertionError("Unsupported IRepository type" + repository.getClass());
    }

    //TODO: use query.stream() once available: https://github.com/eclipse-equinox/p2/pull/312 is available
    private static <T> Stream<T> stream(IQueryResult<T> result) {
        return StreamSupport.stream(result.spliterator(), false);
    }

    public void setIncludeSources(boolean includeAllSource, TargetPlatform targetPlatform) {
        this.includeAllSource = includeAllSource;
        this.targetPlatform = targetPlatform;
    }

    public void setIncludeRequiredBundles(boolean includeRequiredBundles) {
        this.includeRequiredBundles = includeRequiredBundles;
    }

    public void setIncludeRequiredFeatures(boolean includeRequiredFeatures) {
        this.includeRequiredFeatures = includeRequiredFeatures;
    }

    public void setFilterProvided(boolean filterProvided) {
        this.filterProvided = filterProvided;
    }

    public void setAddOnlyProvidingRepoReferences(boolean addOnlyProvidingRepoReferences) {
        this.addOnlyProvidingRepoReferences = addOnlyProvidingRepoReferences;
    }

}
