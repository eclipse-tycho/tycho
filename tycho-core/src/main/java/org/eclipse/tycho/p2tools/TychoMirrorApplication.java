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
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import static java.util.stream.Collectors.groupingBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.RepositoryReference;

public class TychoMirrorApplication extends org.eclipse.tycho.p2tools.copiedfromp2.MirrorApplication {

    private static final String SOURCE_SUFFIX = ".source";
    private static final String FEATURE_GROUP = ".feature.group";
    private final Map<String, String> extraArtifactRepositoryProperties;
    private final List<RepositoryReference> repositoryReferences;
    private boolean includeAllSource;
    private boolean includeRequiredBundles;
    private boolean includeRequiredFeatures;
    private boolean filterProvided;
    private TargetPlatform targetPlatform;

    public TychoMirrorApplication(IProvisioningAgent agent, DestinationRepositoryDescriptor destination) {
        super(agent);
        this.extraArtifactRepositoryProperties = destination.getExtraArtifactRepositoryProperties();
        this.repositoryReferences = destination.getRepositoryReferences();
        this.removeAddedRepositories = false;
    }

    @Override
    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        IArtifactRepository result = super.initializeDestination(toInit, mgr);
        // simple.SimpleArtifactRepository.PUBLISH_PACK_FILES_AS_SIBLINGS is not public
        result.setProperty("publishPackFilesAsSiblings", "true");
        extraArtifactRepositoryProperties.forEach(result::setProperty);
        return result;
    }

    @Override
    protected Slicer createSlicer(SlicingOptions options) {
        Map<String, String> context = options.getFilter();
        boolean includeOptionalDependencies = options.includeOptionalDependencies();
        boolean onlyFilteredRequirements = options.followOnlyFilteredRequirements();
        boolean considerFilter = context != null && context.size() > 1;
        IMetadataRepository repository = getCompositeMetadataRepository();
        return new PermissiveSlicer(repository, context, includeOptionalDependencies, options.isEverythingGreedy(),
                options.forceFilterTo(), options.considerStrictDependencyOnly(), onlyFilteredRequirements) {
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
                        return !considerFilter || filter == null || filter.isMatch(selectionContext);
                    }
                }
                return super.isApplicable(req);
            }

            @Override
            protected boolean isApplicable(IRequirement req) {
                throw new UnsupportedOperationException("should never be called!");
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
        var refs = repositoryReferences.stream().flatMap(TychoMirrorApplication::toSpiRepositoryReferences).toList();
        result.addReferences(refs);
        return result;
    }

    private static Stream<org.eclipse.equinox.p2.repository.spi.RepositoryReference> toSpiRepositoryReferences(
            RepositoryReference rr) {
        return Stream.of(IRepository.TYPE_METADATA, IRepository.TYPE_ARTIFACT).map(type -> {
            URI location = URI.create(rr.getLocation());
            int options = rr.isEnable() ? IRepository.ENABLED : IRepository.NONE;
            return new org.eclipse.equinox.p2.repository.spi.RepositoryReference(location, rr.getName(), type, options);
        });
    }

    @Override
    protected List<IArtifactKey> collectArtifactKeys(Collection<IInstallableUnit> ius, IProgressMonitor monitor)
            throws ProvisionException {
        List<IArtifactKey> keys = super.collectArtifactKeys(ius, monitor);
        if (isFilterProvidedItems()) {
            removeProvidedItems(keys, getArtifactRepositoryManager(), monitor);
        }
        return keys;
    }

    @Override
    protected Set<IInstallableUnit> collectUnits(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor)
            throws ProvisionException {
        Set<IInstallableUnit> units = super.collectUnits(slice, monitor);
        if (isFilterProvidedItems()) {
            removeProvidedItems(units, getMetadataRepositoryManager(), monitor);
        }
        return units;
    }

    private boolean isFilterProvidedItems() {
        return filterProvided && !repositoryReferences.isEmpty();
    }

    private <T> void removeProvidedItems(Collection<T> allElements, IRepositoryManager<T> repoManager,
            IProgressMonitor monitor) throws ProvisionException {
        List<IRepository<T>> referencedRepositories = new ArrayList<>();
        for (RepositoryReference reference : repositoryReferences) {
            try {
                URI location = new URI(reference.getLocation());
                IRepository<T> repository = loadRepository(repoManager, location, monitor);
                referencedRepositories.add(repository);
            } catch (URISyntaxException e) {
                throw new ProvisionException("Can't parse referenced URI!", e);
            }
        }
        allElements.removeIf(e -> referencedRepositories.stream().anyMatch(repo -> contains(repo, e)));
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

}
