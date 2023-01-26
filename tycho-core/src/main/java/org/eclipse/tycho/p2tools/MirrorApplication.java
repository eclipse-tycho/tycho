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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.CollectionResult;
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

public class MirrorApplication extends org.eclipse.tycho.p2tools.copiedfromp2.MirrorApplication {

    private static final String SOURCE_SUFFIX = ".source";
    private final Map<String, String> extraArtifactRepositoryProperties;
    private final List<RepositoryReference> repositoryReferences;
    private boolean includeAllSource;
    private boolean includeRequiredBundles;
    private boolean includeRequiredFeatures;
    private TargetPlatform targetPlatform;

    public MirrorApplication(IProvisioningAgent agent, Map<String, String> extraArtifactRepositoryProperties,
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
                            boolean isFeature = capability.getName().endsWith(".feature.group");
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
                    Map<String, IInstallableUnit> sourceIus = new HashMap<>();
                    targetPlatform.getMetadataRepository().query(QueryUtil.ALL_UNITS, null).forEach(iu -> {
                        if (iu.getId().endsWith(SOURCE_SUFFIX)) {
                            sourceIus.put(iu.getId(), iu);
                        }
                    });
                    for (IInstallableUnit iu : collected) {
                        String sourceId = iu.getId().endsWith(".feature.group")
                                ? iu.getId().replaceAll(".feature.group", SOURCE_SUFFIX)
                                : iu.getId() + SOURCE_SUFFIX;
                        IInstallableUnit sourceUnit = sourceIus.get(sourceId);
                        if (sourceUnit != null) {
                            result.add(sourceUnit);
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
                .flatMap(MirrorApplication::toSpiRepositoryReferences).toList();
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

}
