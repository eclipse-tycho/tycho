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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.p2.tools.RepositoryReference;

public class TychoMirrorApplication extends org.eclipse.tycho.p2tools.copiedfromp2.MirrorApplication {

    private static final String SOURCE_SUFFIX = ".source";
    private static final String FEATURE_GROUP = ".feature.group";
    private final Map<String, String> extraArtifactRepositoryProperties;
    private final List<RepositoryReference> repositoryReferences;
    private boolean includeAllSource;
    private boolean includeRequiredBundles;
    private boolean includeRequiredFeatures;
    private TargetPlatform targetPlatform;
    private boolean filterProvided;

    public TychoMirrorApplication(IProvisioningAgent agent, Map<String, String> extraArtifactRepositoryProperties,
            List<RepositoryReference> repositoryReferences) {
        super(agent);
        this.extraArtifactRepositoryProperties = extraArtifactRepositoryProperties;
        this.repositoryReferences = repositoryReferences;
        this.removeAddedRepositories = false;
    }

    @Override
    protected IArtifactRepository initializeDestination(RepositoryDescriptor toInit, IArtifactRepositoryManager mgr)
            throws ProvisionException {
        IArtifactRepository result = super.initializeDestination(toInit, mgr);
        // simple.SimpleArtifactRepository.PUBLISH_PACK_FILES_AS_SIBLINGS is not public
        result.setProperty("publishPackFilesAsSiblings", "true");
        extraArtifactRepositoryProperties.entrySet()
                .forEach(entry -> result.setProperty(entry.getKey(), entry.getValue()));
        return result;
    }

    @Override
    protected Slicer createSlicer(SlicingOptions options) {
        Map<String, String> context = options.getFilter();
        boolean includeOptionalDependencies = options.includeOptionalDependencies();
        boolean onlyFilteredRequirements = options.followOnlyFilteredRequirements();
        boolean considerFilter = (context != null && context.size() > 1) ? true : false;
        IMetadataRepository repository = getCompositeMetadataRepository();
        return new PermissiveSlicer(repository, context, includeOptionalDependencies, options.isEverythingGreedy(),
                options.forceFilterTo(), options.considerStrictDependencyOnly(), onlyFilteredRequirements) {
            @Override
            protected boolean isApplicable(IInstallableUnit iu, IRequirement req) {
                if ((includeRequiredBundles || includeRequiredFeatures) && QueryUtil.isGroup(iu)) {
                    if (req instanceof IRequiredCapability capability) {
                        if (IInstallableUnit.NAMESPACE_IU_ID.equals(capability.getNamespace())) {
                            boolean isFeature = capability.getName().endsWith(FEATURE_GROUP);
                            if ((isFeature && includeRequiredFeatures) || (!isFeature && includeRequiredBundles)) {
                                if (!includeOptionalDependencies) {
                                    if (req.getMin() == 0) {
                                        return false;
                                    }
                                }
                                IMatchExpression<IInstallableUnit> filter = req.getFilter();
                                if (considerFilter) {
                                    if (onlyFilteredRequirements && filter == null) {
                                        return false;
                                    }
                                    boolean filterMatches = filter == null || filter.isMatch(selectionContext);
                                    if (filterMatches) {
                                    }
                                    return filterMatches;
                                }
                                if (filter == null && onlyFilteredRequirements) {
                                    return false;
                                }
                                return true;
                            }
                        }
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
                    IQueryResult<IInstallableUnit> query = targetPlatform.getMetadataRepository()
                            .query(QueryUtil.ALL_UNITS, null);
                    Map<String, List<IInstallableUnit>> sourceIus = StreamSupport.stream(query.spliterator(), false)
                            .filter(iu -> iu.getId().endsWith(SOURCE_SUFFIX))
                            .collect(Collectors.groupingBy(IInstallableUnit::getId));
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
        List<? extends IRepositoryReference> iRepoRefs = repositoryReferences.stream()
                .flatMap(TychoMirrorApplication::toSpiRepositoryReferences).toList();
        result.addReferences(iRepoRefs);
        return result;
    }

    private static Stream<org.eclipse.equinox.p2.repository.spi.RepositoryReference> toSpiRepositoryReferences(
            RepositoryReference rr) {
        return Stream.of(toSpiRepositoryReference(rr, IRepository.TYPE_METADATA),
                toSpiRepositoryReference(rr, IRepository.TYPE_ARTIFACT));
    }

    private static org.eclipse.equinox.p2.repository.spi.RepositoryReference toSpiRepositoryReference(
            RepositoryReference rr, int type) {
        return new org.eclipse.equinox.p2.repository.spi.RepositoryReference(URI.create(rr.getLocation()), rr.getName(),
                type, rr.isEnable() ? IRepository.ENABLED : IRepository.NONE);
    }

    @Override
    protected List<IArtifactKey> collectArtifactKeys(Collection<IInstallableUnit> ius, IProgressMonitor monitor)
            throws ProvisionException {
        List<IArtifactKey> keys = super.collectArtifactKeys(ius, monitor);
        if (isFilterProvidedItems()) {
            List<IArtifactRepository> referencedRepositories = new ArrayList<>();
            for (RepositoryReference reference : repositoryReferences) {
                String location = reference.getLocation();
                try {
                    referencedRepositories
                            .add(getArtifactRepositoryManager().loadRepository(new URI(location), monitor));
                } catch (URISyntaxException e) {
                    throw new ProvisionException("Can't parse referenced URI!", e);
                }
            }
            keys.removeIf(key -> referencedRepositories.stream().anyMatch(repo -> repo.contains(key)));
        }
        return keys;
    }

    private boolean isFilterProvidedItems() {
        return filterProvided && !repositoryReferences.isEmpty();
    }

    @Override
    protected Set<IInstallableUnit> collectUnits(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor)
            throws ProvisionException {
        Set<IInstallableUnit> units = super.collectUnits(slice, monitor);
        if (isFilterProvidedItems()) {
            List<IMetadataRepository> referencedRepositories = new ArrayList<>();
            for (RepositoryReference reference : repositoryReferences) {
                String location = reference.getLocation();
                try {
                    referencedRepositories
                            .add(getMetadataRepositoryManager().loadRepository(new URI(location), monitor));
                } catch (URISyntaxException e) {
                    throw new ProvisionException("Can't parse referenced URI!", e);
                }
            }
            units.removeIf(unit -> referencedRepositories.stream().anyMatch(repo -> {
                return !repo.query(QueryUtil.createIUQuery(unit.getId(), unit.getVersion()), monitor).isEmpty();
            }));
        }
        return units;
    }

    public void setIncludeSources(boolean includeAllSource, TargetPlatform targetPlatform) {
        this.includeAllSource = includeAllSource;
        this.targetPlatform = targetPlatform;
    }

    public void setIncludeRequiredBundles(boolean includeRequiredBundles) {
        this.includeRequiredBundles = includeRequiredBundles;
    }

    public void setFilterProvided(boolean filterProvided) {
        this.filterProvided = filterProvided;
    }

    public void setIncludeRequiredFeatures(boolean includeRequiredFeatures) {
        this.includeRequiredFeatures = includeRequiredFeatures;
    }

}
