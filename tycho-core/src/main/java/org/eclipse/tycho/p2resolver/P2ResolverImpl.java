/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - add support for IU type (428310)
 *    Christoph Läubrich - Bug 572481 - Tycho does not understand "additional.bundles" directive in build.properties
 *                       - Issue #82  - Support resolving of non-project IUs in P2Resolver
 *                       - Issue #462 - Delay Pom considered items to the final Target Platform calculation
 *                       - Issue #626 - Classpath computation must take fragments into account
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.felix.resolver.util.CopyOnWriteSet;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyResolutionException;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.core.resolver.DefaultP2ResolutionResult;
import org.eclipse.tycho.core.resolver.MetadataOnlyP2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.core.shared.LoggingProgressMonitor;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MultiLineLogger;
import org.eclipse.tycho.p2.publisher.AuthoredIUAction;
import org.eclipse.tycho.p2.resolver.ResolverException;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.p2tools.copiedfromp2.QueryableArray;
import org.eclipse.tycho.p2tools.copiedfromp2.Slicer;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;
import org.eclipse.tycho.version.TychoVersion;

public class P2ResolverImpl implements P2Resolver {

    private final MavenLogger logger;

    private final IProgressMonitor monitor;

    private final List<TargetEnvironment> environments;
    private Map<String, String> additionalFilterProperties = new HashMap<>();

    private final List<IRequirement> additionalRequirements = new ArrayList<>();

    private TargetPlatformFactory targetPlatformFactory;

    private PomDependencies pomDependencies = PomDependencies.ignore;

    private P2ResolverFactoryImpl p2ResolverFactoryImpl;

    public P2ResolverImpl(TargetPlatformFactory targetPlatformFactory, P2ResolverFactoryImpl p2ResolverFactoryImpl,
            MavenLogger logger, Collection<TargetEnvironment> environments) {
        this.targetPlatformFactory = targetPlatformFactory;
        this.p2ResolverFactoryImpl = p2ResolverFactoryImpl;
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor(logger);
        this.environments = List.copyOf(environments);
    }

    @Override
    public Map<TargetEnvironment, P2ResolutionResult> resolveTargetDependencies(TargetPlatform context,
            ReactorProject project) {
        P2TargetPlatform targetPlatform = getTargetFromContext(context);

        // we need a linked hashmap to maintain iteration-order, some of the code relies on it!
        Map<TargetEnvironment, P2ResolutionResult> results = new LinkedHashMap<>();
        Set<IInstallableUnit> usedTargetPlatformUnits = new LinkedHashSet<>();
        Set<IInstallableUnit> usedShadowedUnits = new CopyOnWriteSet<>();
        for (TargetEnvironment environment : environments) {
            results.put(environment,
                    resolveDependencies(Collections.emptySet(), project, new ProjectorResolutionStrategy(logger) {
                        @Override
                        protected Slicer newSlicer(IQueryable<IInstallableUnit> availableUnits,
                                Map<String, String> properties) {
                            return super.newSlicer(
                                    new ShadowedUnitsQueryable(targetPlatform, availableUnits, usedShadowedUnits),
                                    properties);
                        }
                    }, environment, targetPlatform, usedTargetPlatformUnits));
        }
        targetPlatform.reportUsedLocalIUs(usedTargetPlatformUnits);
        for (IInstallableUnit unit : usedShadowedUnits) {
            logger.warn("Your build strictly depends on unit " + unit
                    + " that is shadowed by a reactor project, this can lead to unexpected build results!");
        }
        return results;
    }

    @Override
    public Map<TargetEnvironment, P2ResolutionResult> resolveArtifactDependencies(TargetPlatform context,
            Collection<? extends ArtifactKey> artifacts) {
        P2TargetPlatform targetPlatform = getTargetFromContext(context);
        Collection<IInstallableUnit> roots = new ArrayList<>();
        IQueryable<IInstallableUnit> queriable = new QueryableArray(targetPlatform.getInstallableUnits());
        for (ArtifactKey artifactKey : artifacts) {
            VersionRange range = new VersionRange(artifactKey.getVersion());
            IQuery<IInstallableUnit> query = ArtifactTypeHelper.createQueryFor(artifactKey.getType(),
                    artifactKey.getId(), range);
            IQueryResult<IInstallableUnit> result = queriable.query(QueryUtil.createLatestQuery(query), monitor);
            roots.addAll(result.toUnmodifiableSet());
        }
        Map<TargetEnvironment, P2ResolutionResult> results = new LinkedHashMap<>();
        for (TargetEnvironment environment : environments) {
            results.put(environment, resolveDependencies(roots, null, new ProjectorResolutionStrategy(logger),
                    environment, targetPlatform, null));
        }
        return results;
    }

    @Override
    public P2ResolutionResult resolveMetadata(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfig) {
        P2TargetPlatform contextImpl = (P2TargetPlatform) targetPlatformFactory.createTargetPlatform(tpConfiguration,
                eeConfig, null);

        ResolutionDataImpl data = new ResolutionDataImpl(contextImpl.getEEResolutionHints());
        data.setAvailableIUs(contextImpl.getInstallableUnits());
        data.setRootIUs(new HashSet<>());
        data.setAdditionalRequirements(additionalRequirements);

        ProjectorResolutionStrategy strategy = new ProjectorResolutionStrategy(logger);
        strategy.setData(data);

        MetadataOnlyP2ResolutionResult result = new MetadataOnlyP2ResolutionResult();
        try {
            for (IInstallableUnit iu : strategy.multiPlatformResolve(environments, monitor)) {
                result.addArtifact(ArtifactType.TYPE_INSTALLABLE_UNIT, iu.getId(), iu.getVersion().toString(), iu);
            }
        } catch (ResolverException e) {
            logger.error("Resolution failed:");
            new MultiLineLogger(logger).error(e.getDetails(), "  ");
            throw new RuntimeException(e);
        }
        return result;
    }

    // TODO 412416 make this obsolete by adding appropriate getters in TargetPlatform interface
    @Override
    public P2ResolutionResult getTargetPlatformAsResolutionResult(TargetPlatformConfigurationStub tpConfiguration,
            String eeName) {
        P2TargetPlatform targetPlatform = (P2TargetPlatform) targetPlatformFactory.createTargetPlatform(tpConfiguration,
                new ExecutionEnvironmentConfigurationStub(eeName), null);

        MetadataOnlyP2ResolutionResult result = new MetadataOnlyP2ResolutionResult();
        for (IInstallableUnit iu : targetPlatform.getInstallableUnits()) {
            result.addArtifact(ArtifactType.TYPE_INSTALLABLE_UNIT, iu.getId(), iu.getVersion().toString(), iu);
        }
        return result;
    }

    protected P2ResolutionResult resolveDependencies(Collection<IInstallableUnit> rootUIs, ReactorProject project,
            AbstractResolutionStrategy strategy, TargetEnvironment environment, P2TargetPlatform targetPlatform,
            Set<IInstallableUnit> usedTargetPlatformUnits) {
        ResolutionDataImpl data = new ResolutionDataImpl(targetPlatform.getEEResolutionHints());

        Set<IInstallableUnit> availableUnits = targetPlatform.getInstallableUnits();
        if (project != null) {
            data.setRootIUs(project.getDependencyMetadata(DependencyMetadataType.SEED));
            Collection<IInstallableUnit> projectSecondaryIUs = project
                    .getDependencyMetadata(DependencyMetadataType.RESOLVE);
            if (!projectSecondaryIUs.isEmpty()) {
                availableUnits = new LinkedHashSet<>(availableUnits);
                availableUnits.addAll(projectSecondaryIUs);
            }
        } else {
            data.setRootIUs(rootUIs);
        }
        data.setAdditionalRequirements(additionalRequirements);
        data.setAvailableIUs(availableUnits);
        data.setAdditionalFilterProperties(additionalFilterProperties);

        strategy.setData(data);
        Collection<IInstallableUnit> newState;
        try {
            if (project != null && p2ResolverFactoryImpl != null && pomDependencies != PomDependencies.ignore) {
                data.setAdditionalUnitStore(p2ResolverFactoryImpl.getPomUnits().createPomQueryable(project));
            }
            newState = strategy.resolve(environment, monitor);
        } catch (ResolverException e) {
            logger.info(e.getSelectionContext());
            logger.error("Cannot resolve project dependencies:");
            new MultiLineLogger(logger).error(e.getDetails(), "  ");
            logger.error("");
            String version = TychoVersion.getTychoVersion();
            // Use 'main' for SNAPSHOT versions, otherwise use the actual version
            String docVersion = version.contains("-SNAPSHOT") ? "main" : version;
            logger.error("See https://tycho.eclipseprojects.io/doc/" + docVersion + "/Troubleshooting.html for help.");
            throw new DependencyResolutionException("Cannot resolve dependencies of " + project, e);
        }

        if (usedTargetPlatformUnits != null) {
            usedTargetPlatformUnits.addAll(newState);
        }
        Set<IInstallableUnit> dependencyFragments = p2ResolverFactoryImpl == null ? Collections.emptySet()
                : p2ResolverFactoryImpl.calculateDependencyFragments(data, newState);
        return toResolutionResult(newState, dependencyFragments, project, targetPlatform);
    }

    private P2ResolutionResult toResolutionResult(Collection<IInstallableUnit> resolvedUnits,
            Collection<IInstallableUnit> dependencyFragments, ReactorProject project, P2TargetPlatform targetPlatform) {
        Set<IInstallableUnit> currentProjectUnits = getProjectUnits(project);
        DefaultP2ResolutionResult result = new DefaultP2ResolutionResult(dependencyFragments, targetPlatform);

        for (IInstallableUnit iu : resolvedUnits) {
            addUnit(result, iu, project, targetPlatform, currentProjectUnits);
        }
        // remove entries for which there were only "additional" IUs, but none with a recognized type
        result.removeEntriesWithUnknownType();

        // local repository index needs to be saved manually
        targetPlatform.saveLocalMavenRepository();

        // TODO 372780 remove; no longer needed when aggregation uses frozen target platform as source
        collectNonReactorIUs(result, resolvedUnits, targetPlatform, currentProjectUnits);
        return result;
    }

    private static void addUnit(DefaultP2ResolutionResult result, IInstallableUnit iu, ReactorProject project,
            P2TargetPlatform targetPlatform, Set<IInstallableUnit> currentProjectUnits) {

        if (currentProjectUnits.contains(iu)) {
            addReactorProject(result, project.getIdentities(), iu);
            return;
        }

        ReactorProjectIdentities otherProject = targetPlatform.getOriginalReactorProjectMap().get(iu);
        if (otherProject != null) {
            addReactorProject(result, otherProject, iu);
            return;
        }

        IArtifactFacade mavenArtifact = targetPlatform.getOriginalMavenArtifactMap().get(iu);
        if (mavenArtifact != null) {
            addExternalMavenArtifact(result, mavenArtifact, iu);
            return;
        }

        for (IArtifactKey key : iu.getArtifacts()) {
            addArtifactFile(result, iu, key, targetPlatform);
        }
    }

    @Override
    public void setAdditionalFilterProperties(Map<String, String> additionalFilterProperties) {
        Objects.requireNonNull(additionalFilterProperties, "additionalFilterProperties can't be null");
        this.additionalFilterProperties = additionalFilterProperties;
    }

    @Override
    public void addDependency(String type, String id, String versionRange) throws IllegalArtifactReferenceException {
        final VersionRange parsedVersionRange;
        try {
            parsedVersionRange = new VersionRange(versionRange);
        } catch (IllegalArgumentException e) {
            throw new IllegalArtifactReferenceException(
                    "The string \"" + versionRange + "\" is not a valid OSGi version range");
        }
        additionalRequirements.add(ArtifactTypeHelper.createRequirementFor(type, id, parsedVersionRange));
    }

    @Override
    public void addRequirement(IRequirement requirement) {
        additionalRequirements.add(requirement);
    }

    @Override
    public void addAdditionalBundleDependency(String bundleId) {
        additionalRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, bundleId,
                VersionRange.emptyRange, null, false, true, true));

    }

    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

    // TODO 412416 this should be a method on the class TargetPlatform
    @Override
    public P2ResolutionResult resolveInstallableUnit(TargetPlatform context, String id, String versionRange) {

        P2TargetPlatform targetPlatform = getTargetFromContext(context);
        IQueryable<IInstallableUnit> queriable = new QueryableArray(targetPlatform.getInstallableUnits(), false);

        VersionRange range = new VersionRange(versionRange);
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range, null,
                1 /* min */, Integer.MAX_VALUE /* max */, false /* greedy */);

        IQueryResult<IInstallableUnit> result = queriable
                .query(QueryUtil.createLatestQuery(QueryUtil.createMatchQuery(requirement.getMatches())), monitor);

        Set<IInstallableUnit> newState = result.toUnmodifiableSet();

        return toResolutionResult(newState, Collections.emptyList(), null, targetPlatform);
    }

    private static P2TargetPlatform getTargetFromContext(TargetPlatform context) {
        Objects.requireNonNull(context, "target context can't be null");
        if (context instanceof P2TargetPlatform p2TP) {
            return p2TP;
        }
        throw new IllegalArgumentException("invalid target context-type " + context.getClass().getName()
                + ", required is " + P2TargetPlatform.class);
    }

    private static Set<IInstallableUnit> getProjectUnits(ReactorProject project) {
        if (project == null) {
            return Collections.emptySet();
        } else {
            return project.getDependencyMetadata();
        }
    }

    private static IRequirement optionalGreedyRequirementTo(String bundleId) {
        return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, bundleId, VersionRange.emptyRange,
                null, true, true, true);
    }

    private static boolean isPureIU(IInstallableUnit iu) {
        return Boolean.parseBoolean(iu.getProperty(AuthoredIUAction.IU_TYPE));
    }

    private static boolean isBundleOrFragmentWithId(IInstallableUnit iu, String id) {
        for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
            if (BundlesAction.CAPABILITY_NS_OSGI_BUNDLE.equals(provided.getNamespace())) {
                return id.equals(provided.getName());
            }
        }
        return false;
    }

    private static boolean isProduct(IInstallableUnit iu) {
        return Boolean.parseBoolean(iu.getProperty(InstallableUnitDescription.PROP_TYPE_PRODUCT));
    }

    private static void addExternalMavenArtifact(DefaultP2ResolutionResult result, IArtifactFacade mavenArtifact,
            IInstallableUnit iu) {
        String id = iu.getId();
        String version = iu.getVersion().toString();
        String mavenClassifier = iu.getProperty(TychoConstants.PROP_CLASSIFIER);
        File location = mavenArtifact.getLocation();

        addMavenArtifact(result, iu, id, version, mavenClassifier, location);
    }

    protected static void addMavenArtifact(DefaultP2ResolutionResult result, IInstallableUnit iu, String id,
            String version, String mavenClassifier, File location) {
        final String contributingArtifactType;
        final String contributingArtifactId;

        // TODO 353889 infer the type from the p2 artifact as this is done for content from p2 repositories; this would prevent bug 430728
        if (isBundleOrFragmentWithId(iu, id)) {
            contributingArtifactType = ArtifactType.TYPE_ECLIPSE_PLUGIN;
            contributingArtifactId = id;
        } else {
            String featureId = ArtifactTypeHelper.getFeatureId(iu);
            if (featureId != null) {
                contributingArtifactType = ArtifactType.TYPE_ECLIPSE_FEATURE;
                // feature can have additional IUs injected via p2.inf
                contributingArtifactId = featureId;
            } else if (isProduct(iu)) {
                contributingArtifactType = ArtifactType.TYPE_ECLIPSE_PRODUCT;
                contributingArtifactId = id;
            } else if (isPureIU(iu)) {
                contributingArtifactType = ArtifactType.TYPE_INSTALLABLE_UNIT;
                contributingArtifactId = id;
            } else {
                // additional IU of an artifact/project -> will be added to the artifact/project by its location
                contributingArtifactType = null;
                contributingArtifactId = null;
            }
        }

        result.addResolvedArtifact(
                Optional.ofNullable(contributingArtifactId)
                        .map(artifactId -> new DefaultArtifactKey(contributingArtifactType, artifactId, version)),
                mavenClassifier, iu, location);
    }

    private static void collectNonReactorIUs(DefaultP2ResolutionResult result, Collection<IInstallableUnit> newState,
            P2TargetPlatform targetPlatform, Set<IInstallableUnit> currentProjectUnits) {
        Map<IInstallableUnit, ReactorProjectIdentities> reactorProjectLookup = targetPlatform
                .getOriginalReactorProjectMap();

        for (IInstallableUnit iu : newState) {
            if (!currentProjectUnits.contains(iu) && reactorProjectLookup.get(iu) == null) {
                result.addNonReactorUnit(iu);
            }
        }
    }

    private static void addReactorProject(DefaultP2ResolutionResult result, ReactorProjectIdentities project,
            IInstallableUnit iu) {
        String id = iu.getId();
        String version = iu.getVersion().toString();
        String mavenClassifier = iu.getProperty(TychoConstants.PROP_CLASSIFIER);
        File location = project.getBasedir();

        addMavenArtifact(result, iu, id, version, mavenClassifier, location);
    }

    private static void addArtifactFile(DefaultP2ResolutionResult result, IInstallableUnit iu,
            IArtifactKey p2ArtifactKey, P2TargetPlatform context) {
        String mavenClassifier = iu.getProperty("maven-classifier");
        if (mavenClassifier != null && mavenClassifier.isBlank()) {
            mavenClassifier = null;
        }
        if (mavenClassifier == null) {
            mavenClassifier = ArtifactTypeHelper.toMavenClassifier(iu);
        }
        ArtifactKey artifactKey = ArtifactTypeHelper.toTychoArtifactKey(iu, p2ArtifactKey);
        if (artifactKey != null) {
            result.addArtifact(artifactKey, mavenClassifier, iu, p2ArtifactKey);
        }

        // ignore other/unknown artifacts, like binary blobs for now.
        // throw new IllegalArgumentException();
    }

    @Override
    public void setPomDependencies(PomDependencies pomDependencies) {
        this.pomDependencies = pomDependencies;
    }

}
