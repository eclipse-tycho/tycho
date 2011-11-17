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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.ResolutionContext;
import org.eclipse.tycho.repository.registry.facade.RepositoryBlackboardKey;
import org.eclipse.tycho.repository.registry.impl.ArtifactRepositoryBlackboard;

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

    // TODO provide needed methods through adapter interface? (to avoid cast to implementation)
    private ResolutionContextImpl context;

    public P2ResolverImpl(MavenLogger logger) {
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor(logger);
    }

    public List<P2ResolutionResult> resolveProject(ResolutionContext context, File projectLocation) {
        this.context = (ResolutionContextImpl) context;

        ArrayList<P2ResolutionResult> results = new ArrayList<P2ResolutionResult>();

        for (Map<String, String> properties : environments) {
            results.add(resolveProject(projectLocation, new ProjectorResolutionStrategy(properties, logger)));
        }

        return results;
    }

    public P2ResolutionResult collectProjectDependencies(ResolutionContext context, File projectLocation) {
        this.context = (ResolutionContextImpl) context;
        return resolveProject(projectLocation, new DependencyCollector(logger));
    }

    public P2ResolutionResult resolveMetadata(ResolutionContext context, Map<String, String> properties) {
        ProjectorResolutionStrategy strategy = new ProjectorResolutionStrategy(properties, logger);
        ResolutionContextImpl contextImpl = (ResolutionContextImpl) context;
        strategy.setJREUIs(contextImpl.getJREIUs());
        strategy.setAvailableInstallableUnits(contextImpl.gatherAvailableInstallableUnits(monitor));
        strategy.setRootInstallableUnits(new HashSet<IInstallableUnit>());
        strategy.setAdditionalRequirements(additionalRequirements);

        P2ResolutionResult result = new P2ResolutionResult();
        for (IInstallableUnit iu : strategy.resolve(monitor)) {
            result.addArtifact(TYPE_INSTALLABLE_UNIT, iu.getId(), iu.getVersion().toString(), null, null, iu);
        }
        return result;
    }

    protected P2ResolutionResult resolveProject(File projectLocation, ResolutionStrategy strategy) {
        context.assertNoDuplicateReactorUIs();

        strategy.setAvailableInstallableUnits(context.gatherAvailableInstallableUnits(monitor));
        strategy.setJREUIs(context.getJREIUs());
        LinkedHashSet<IInstallableUnit> projectIUs = context.getReactorProjectIUs(projectLocation);
        strategy.setRootInstallableUnits(projectIUs);
        strategy.setAdditionalRequirements(additionalRequirements);

        Collection<IInstallableUnit> newState = strategy.resolve(monitor);
        context.warnAboutLocalIus(newState);

        context.downloadArtifacts(newState);

        // TODO check if needed by all callers
        IArtifactRepository resolutionContextArtifactRepo = context.getSupplementaryArtifactRepository();
        RepositoryBlackboardKey blackboardKey = RepositoryBlackboardKey.forResolutionContextArtifacts(projectLocation);
        ArtifactRepositoryBlackboard.putRepository(blackboardKey, resolutionContextArtifactRepo);
        logger.debug("Registered artifact repository " + blackboardKey);

        return toResolutionResult(newState);
    }

    private P2ResolutionResult toResolutionResult(Collection<IInstallableUnit> newState) {
        P2ResolutionResult result = new P2ResolutionResult();
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

    private void collectNonReactorIUs(P2ResolutionResult result, Collection<IInstallableUnit> newState) {
        for (IInstallableUnit iu : newState) {
            if (!isReactorArtifact(iu)) {
                result.addNonReactorUnit(iu);
            }
        }
    }

    private boolean isReactorArtifact(IInstallableUnit iu) {
        return context.getMavenArtifact(iu) instanceof IReactorArtifactFacade;
    }

    private void addArtifactFile(P2ResolutionResult platform, IInstallableUnit iu, IArtifactKey key) {
        File file = context.getLocalArtifactFile(key);
        if (file == null) {
            return;
        }

        IArtifactFacade reactorArtifact = context.getMavenArtifact(iu);

        String id = iu.getId();
        String version = iu.getVersion().toString();
        String mavenClassidier = reactorArtifact != null ? reactorArtifact.getClassidier() : null;

        if (PublisherHelper.OSGI_BUNDLE_CLASSIFIER.equals(key.getClassifier())) {
            platform.addArtifact(P2Resolver.TYPE_ECLIPSE_PLUGIN, id, version, file, mavenClassidier, iu);
        } else if (PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER.equals(key.getClassifier())) {
            String featureId = getFeatureId(iu);
            if (featureId != null) {
                platform.addArtifact(P2Resolver.TYPE_ECLIPSE_FEATURE, featureId, version, file, mavenClassidier, iu);
            }
        }

        // ignore other/unknown artifacts, like binary blobs for now.
        // throw new IllegalArgumentException();
    }

    private void addMavenArtifact(P2ResolutionResult platform, IArtifactFacade mavenArtifact, IInstallableUnit iu) {
        String type = mavenArtifact.getPackagingType();
        String id = iu.getId();
        String version = iu.getVersion().toString();
        File location = mavenArtifact.getLocation();
        String mavenClassidier = mavenArtifact.getClassidier();

        if (TYPE_ECLIPSE_FEATURE.equals(type)) {
            id = getFeatureId(iu);
            if (id == null) {
                throw new IllegalStateException("Feature id is null for maven artifact at "
                        + mavenArtifact.getLocation() + " with classifier " + mavenArtifact.getClassidier());
            }
        } else if ("jar".equals(type)) {
            // this must be an OSGi bundle coming from a maven repository
            // TODO check if iu actually provides CAPABILITY_NS_OSGI_BUNDLE capability
            type = TYPE_ECLIPSE_PLUGIN;
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
        } else if (P2Resolver.TYPE_ECLIPSE_PLUGIN.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, id,
                    new VersionRange(versionRange), null, false, true));
        } else if (P2Resolver.TYPE_ECLIPSE_FEATURE.equals(type)) {
            additionalRequirements.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id
                    + ".feature.group", new VersionRange(versionRange), null, false, true));
            // TODO make ".feature.group" a constant in FeaturesAction
        }
        // TODO else throw an exception
    }

    public List<IRequirement> getAdditionalRequirements() {
        return additionalRequirements;
    }

}
