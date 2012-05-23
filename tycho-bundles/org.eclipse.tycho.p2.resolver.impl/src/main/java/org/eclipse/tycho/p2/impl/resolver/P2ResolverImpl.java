/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.artifacts.p2.P2TargetPlatform;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;

@SuppressWarnings("restriction")
public class P2ResolverImpl implements P2Resolver {
    // BundlesAction.CAPABILITY_NS_OSGI_BUNDLE
    private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle";

    private final MavenLogger logger;

    private final IProgressMonitor monitor;

    /**
     * Target runtime environment properties
     */
    private List<Map<String, String>> environments;

    private final List<IRequirement> additionalRequirements = new ArrayList<IRequirement>();

    private P2TargetPlatform context;

    private Set<IInstallableUnit> usedTargetPlatformUnits;

    public P2ResolverImpl(MavenLogger logger) {
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor(logger);
    }

    public List<P2ResolutionResult> resolveProject(TargetPlatform targetPlatform, File projectLocation) {
        this.context = (P2TargetPlatform) targetPlatform;

        ArrayList<P2ResolutionResult> results = new ArrayList<P2ResolutionResult>();
        usedTargetPlatformUnits = new LinkedHashSet<IInstallableUnit>();

        for (Map<String, String> properties : environments) {
            results.add(resolveProject(projectLocation, new ProjectorResolutionStrategy(logger), properties));
        }

        context.reportUsedIUs(usedTargetPlatformUnits);
        usedTargetPlatformUnits = null;

        return results;
    }

    public P2ResolutionResult collectProjectDependencies(TargetPlatform context, File projectLocation) {
        this.context = (P2TargetPlatform) context;
        return resolveProject(projectLocation, new DependencyCollector(logger), Collections.<String, String> emptyMap());
    }

    public P2ResolutionResult resolveMetadata(TargetPlatformBuilder context, Map<String, String> properties) {
        ProjectorResolutionStrategy strategy = new ProjectorResolutionStrategy(logger);
        P2TargetPlatform contextImpl = (P2TargetPlatform) context.buildTargetPlatform();
        strategy.setJREUIs(contextImpl.getJREIUs());
        strategy.setAvailableInstallableUnits(contextImpl.getInstallableUnits());
        strategy.setRootInstallableUnits(new HashSet<IInstallableUnit>());
        strategy.setAdditionalRequirements(additionalRequirements);

        MetadataOnlyP2ResolutionResult result = new MetadataOnlyP2ResolutionResult();
        for (IInstallableUnit iu : strategy.resolve(environments, monitor)) {
            result.addArtifact(TYPE_INSTALLABLE_UNIT, iu.getId(), iu.getVersion().toString(), iu);
        }
        return result;
    }

    protected P2ResolutionResult resolveProject(File projectLocation, AbstractResolutionStrategy strategy,
            Map<String, String> properties) {
        strategy.setRootInstallableUnits(context.getReactorProjectIUs(projectLocation, true));
        strategy.setAdditionalRequirements(additionalRequirements);
        Collection<IInstallableUnit> availableUnits = context.getInstallableUnits();
        Collection<IInstallableUnit> projectSecondaryIUs = context.getReactorProjectIUs(projectLocation, false);
        if (!projectSecondaryIUs.isEmpty()) {
            availableUnits = new LinkedHashSet<IInstallableUnit>(availableUnits);
            availableUnits.addAll(projectSecondaryIUs);
        }
        strategy.setAvailableInstallableUnits(availableUnits);
        strategy.setJREUIs(context.getJREIUs());

        Collection<IInstallableUnit> newState = strategy.resolve(properties, monitor);

        if (usedTargetPlatformUnits != null) {
            usedTargetPlatformUnits.addAll(newState);
        }

        context.downloadArtifacts(newState);
        return toResolutionResult(newState);
    }

    private P2ResolutionResult toResolutionResult(Collection<IInstallableUnit> newState) {
        DefaultP2ResolutionResult result = new DefaultP2ResolutionResult();
        for (IInstallableUnit iu : newState) {
            IArtifactFacade mavenArtifact = context.getMavenArtifact(iu);
            if (mavenArtifact != null) {
                addMavenArtifact(result, mavenArtifact, iu);
            } else {
                for (IArtifactKey key : iu.getArtifacts()) {
                    addArtifactFile(result, iu, key);
                }
            }
        }

        // TODO instead of adding them to the TP, we could also register it in memory as metadata repo
        collectNonReactorIUs(result, newState);
        return result;
    }

    private void collectNonReactorIUs(DefaultP2ResolutionResult result, Collection<IInstallableUnit> newState) {
        for (IInstallableUnit iu : newState) {
            if (!isReactorArtifact(iu)) {
                result.addNonReactorUnit(iu);
            }
        }
    }

    private boolean isReactorArtifact(IInstallableUnit iu) {
        return context.getMavenArtifact(iu) instanceof IReactorArtifactFacade;
    }

    private void addArtifactFile(DefaultP2ResolutionResult platform, IInstallableUnit iu, IArtifactKey key) {
        File file = context.getLocalArtifactFile(key);
        if (file == null) {
            return;
        }

        IArtifactFacade reactorArtifact = context.getMavenArtifact(iu);

        String id = iu.getId();
        String version = iu.getVersion().toString();
        String mavenClassidier = reactorArtifact != null ? reactorArtifact.getClassifier() : null;

        if (PublisherHelper.OSGI_BUNDLE_CLASSIFIER.equals(key.getClassifier())) {
            platform.addArtifact(ArtifactKey.TYPE_ECLIPSE_PLUGIN, id, version, file, mavenClassidier, iu);
        } else if (PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER.equals(key.getClassifier())) {
            String featureId = getFeatureId(iu);
            if (featureId != null) {
                platform.addArtifact(ArtifactKey.TYPE_ECLIPSE_FEATURE, featureId, version, file, mavenClassidier, iu);
            }
        }

        // ignore other/unknown artifacts, like binary blobs for now.
        // throw new IllegalArgumentException();
    }

    private void addMavenArtifact(DefaultP2ResolutionResult platform, IArtifactFacade mavenArtifact, IInstallableUnit iu) {
        String type = mavenArtifact.getPackagingType();
        String id = iu.getId();
        String version = iu.getVersion().toString();
        File location = mavenArtifact.getLocation();
        String mavenClassidier = mavenArtifact.getClassifier();

        if (ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(type)) {
            String featureId = getFeatureId(iu);
            if (featureId != null) {
                // feature can have additional IUs injected via p2.inf
                id = featureId;
            }
        } else if ("jar".equals(type)) {
            // this must be an OSGi bundle coming from a maven repository
            // TODO check if iu actually provides CAPABILITY_NS_OSGI_BUNDLE capability
            type = ArtifactKey.TYPE_ECLIPSE_PLUGIN;
        }

        platform.addArtifact(type, id, version, location, mavenClassidier, iu);
    }

    private String getFeatureId(IInstallableUnit iu) {
        for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
            if (PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE.equals(provided.getNamespace())) {
                return provided.getName();
            }
        }
        return null;
    }

    public void setEnvironments(List<Map<String, String>> environments) {
        this.environments = environments;
    }

    public void addDependency(String type, String id, String versionRange) {
        if (P2Resolver.TYPE_INSTALLABLE_UNIT.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id,
                    new VersionRange(versionRange), null, false, true));
        } else if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, id,
                    new VersionRange(versionRange), null, false, true));
        } else if (ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id
                    + ".feature.group", new VersionRange(versionRange), null, false, true));
            // TODO make ".feature.group" a constant in FeaturesAction
        }
        // TODO else throw an exception
    }

    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

    public P2ResolutionResult resolveInstallableUnit(TargetPlatform context, String id, String version) {
        this.context = (P2TargetPlatform) context;

        QueryableCollection queriable = new QueryableCollection(((P2TargetPlatform) context).getInstallableUnits());

        Version v = Version.create(version);
        VersionRange range = new VersionRange(v, true, v, true);
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range, null,
                1 /* min */, 1 /* max */, false /* greedy */);

        IQueryResult<IInstallableUnit> result = queriable.query(QueryUtil.createMatchQuery(requirement.getMatches()),
                monitor);

        Set<IInstallableUnit> newState = result.toUnmodifiableSet();

        this.context.downloadArtifacts(newState);

        return toResolutionResult(newState);
    }
}
