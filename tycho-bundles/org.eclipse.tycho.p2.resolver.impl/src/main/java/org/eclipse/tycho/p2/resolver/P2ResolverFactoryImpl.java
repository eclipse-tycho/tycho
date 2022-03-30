/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *                         Issue #443 - Use regular Maven coordinates -when possible- for dependencies 
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverService;
import org.eclipse.tycho.p2.target.TargetPlatformFactoryImpl;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.util.resolution.ResolutionData;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;

@SuppressWarnings("restriction")
public class P2ResolverFactoryImpl implements P2ResolverFactory {

    // TODO cache these instances in an p2 agent, and not here
    private static LocalMetadataRepository localMetadataRepository;
    private static LocalArtifactRepository localArtifactRepository;

    private MavenContext mavenContext;
    private LocalRepositoryP2Indices localRepoIndices;
    private RemoteAgentManager remoteAgentManager;
    private TargetDefinitionResolverService targetDefinitionResolverService;
    private ConcurrentMap<IInstallableUnit, Optional<Entry<IInstallableUnit, IRequiredCapability>>> hostRequirementMap = new ConcurrentHashMap<>();

    private static synchronized LocalMetadataRepository getLocalMetadataRepository(MavenContext context,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localMetadataRepository == null) {
            File localMavenRepoRoot = context.getLocalRepositoryRoot();
            RepositoryReader contentLocator = new LocalRepositoryReader(context);
            localMetadataRepository = new LocalMetadataRepository(localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), contentLocator);

        }
        return localMetadataRepository;
    }

    private static synchronized LocalArtifactRepository getLocalArtifactRepository(MavenContext mavenContext,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localArtifactRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(mavenContext);
            localArtifactRepository = new LocalArtifactRepository(localRepoIndices, contentLocator);
        }
        return localArtifactRepository;
    }

    @Override
    public PomDependencyCollector newPomDependencyCollector(ReactorProject project) {
        return new PomDependencyCollectorImpl(mavenContext, project);
    }

    @Override
    public TargetPlatformFactoryImpl getTargetPlatformFactory() {
        try {
            // TODO don't synchronize twice
            LocalMetadataRepository localMetadataRepo = getLocalMetadataRepository(mavenContext, localRepoIndices);
            LocalArtifactRepository localArtifactRepo = getLocalArtifactRepository(mavenContext, localRepoIndices);
            return new TargetPlatformFactoryImpl(mavenContext, remoteAgentManager.getProvisioningAgent(),
                    localArtifactRepo, localMetadataRepo, targetDefinitionResolverService);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public P2ResolverImpl createResolver(MavenLogger logger) {
        return new P2ResolverImpl(getTargetPlatformFactory(), this, logger);
    }

    public Set<IInstallableUnit> calculateDependencyFragments(ResolutionData data,
            Collection<IInstallableUnit> resolvedUnits) {
        Collection<IInstallableUnit> availableIUs = data.getAvailableIUs();
        List<Entry<IInstallableUnit, IRequiredCapability>> fragmentsList = availableIUs.stream()//
                .map(iu -> hostRequirementMap.computeIfAbsent(iu, key -> {
                    for (IProvidedCapability capability : iu.getProvidedCapabilities()) {
                        String nameSpace = capability.getNamespace();
                        if (BundlesAction.CAPABILITY_NS_OSGI_FRAGMENT.equals(nameSpace)) {
                            String fragmentName = capability.getName();
                            return findFragmentHostRequirement(iu, fragmentName);
                        }
                    }
                    return Optional.empty();

                }))//
                .filter(Optional::isPresent)//
                .map(Optional::get)//
                .collect(Collectors.toCollection(ArrayList::new));
        if (fragmentsList.isEmpty()) {
            return Collections.emptySet();
        }
        Set<IInstallableUnit> dependencyFragments = new HashSet<>();
        for (Iterator<IInstallableUnit> iterator = resolvedUnits.iterator(); iterator.hasNext()
                && !fragmentsList.isEmpty();) {
            IInstallableUnit resolvedUnit = iterator.next();
            addMatchingFragments(fragmentsList, dependencyFragments, resolvedUnit);
        }
        return dependencyFragments;
    }

    private static void addMatchingFragments(List<Entry<IInstallableUnit, IRequiredCapability>> fragmentsList,
            Set<IInstallableUnit> dependencyFragments, IInstallableUnit unitToMatch) {
        Iterator<Entry<IInstallableUnit, IRequiredCapability>> iterator = fragmentsList.iterator();
        while (iterator.hasNext()) {
            Entry<IInstallableUnit, IRequiredCapability> fragment = iterator.next();
            if (fragment.getValue().isMatch(unitToMatch)) {
                dependencyFragments.add(fragment.getKey());
                iterator.remove();
            }
        }
    }

    private static Optional<Entry<IInstallableUnit, IRequiredCapability>> findFragmentHostRequirement(
            IInstallableUnit unit, String fragmentName) {
        for (IRequirement requirement : unit.getRequirements()) {
            if (requirement instanceof IRequiredCapability) {
                IRequiredCapability requiredCapability = (IRequiredCapability) requirement;
                if (fragmentName.equals(requiredCapability.getName())) {
                    return Optional.of(new SimpleEntry<>(unit, requiredCapability));
                }
            }
        }
        return Optional.empty();
    }

    // setters for DS

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setLocalRepositoryIndices(LocalRepositoryP2Indices localRepoIndices) {
        this.localRepoIndices = localRepoIndices;
    }

    public void setRemoteAgentManager(RemoteAgentManager remoteAgentManager) {
        this.remoteAgentManager = remoteAgentManager;
    }

    public void setTargetDefinitionResolverService(TargetDefinitionResolverService targetDefinitionResolverService) {
        this.targetDefinitionResolverService = targetDefinitionResolverService;
    }

    @Override
    public MavenDependencyDescriptor resolveDependencyDescriptor(ArtifactDescriptor artifactDescriptor) {
        return artifactDescriptor.getInstallableUnits().stream().filter(IInstallableUnit.class::isInstance)
                .map(IInstallableUnit.class::cast).map(iu -> {
                    Map<String, String> properties = iu.getProperties();
                    String groupId = properties.get(TychoConstants.PROP_GROUP_ID);
                    String artifactId = properties.get(TychoConstants.PROP_ARTIFACT_ID);
                    String version = properties.get(TychoConstants.PROP_VERSION);
                    if (groupId == null || artifactId == null || version == null) {
                        //these properties are required!
                        return null;
                    }
                    return new MavenDependencyDescriptor() {

                        @Override
                        public String getVersion() {
                            return version;
                        }

                        @Override
                        public String getType() {
                            return properties.get(TychoConstants.PROP_EXTENSION);
                        }

                        @Override
                        public String getGroupId() {
                            return groupId;
                        }

                        @Override
                        public String getClassifier() {
                            return properties.get(TychoConstants.PROP_CLASSIFIER);
                        }

                        @Override
                        public String getArtifactId() {
                            return artifactId;
                        }

                        @Override
                        public String getRepository() {
                            return properties.get(TychoConstants.PROP_REPOSITORY);
                        }
                    };
                }).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
