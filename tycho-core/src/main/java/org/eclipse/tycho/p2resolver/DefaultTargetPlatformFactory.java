/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
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
 *                         Issue #822 - If multiple fragments match a bundle all items are added to the classpath while only the one with the highest version should match
 *                         Extracted into DefaultTargetPlatformFactory
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.MavenBundleResolver;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;

@Named
@Singleton
public class DefaultTargetPlatformFactory implements TargetPlatformFactory {

    // TODO cache these instances in an p2 agent, and not here
    private LocalMetadataRepository localMetadataRepository;
    private LocalArtifactRepository localArtifactRepository;

    @Inject
    private IProvisioningAgent agent;

    @Inject
    private MavenContext mavenContext;

    @Inject
    private LocalRepositoryP2Indices localRepoIndices;

    @Inject
    private IRepositoryIdManager repositoryIdManager;

    @Inject
    private MavenBundleResolver bundleResolver;

    @Inject
    private TychoProjectManager projectManager;

    @Inject
    private TargetDefinitionResolverService targetDefinitionResolverService;
    private TargetPlatformFactoryImpl impl;

    @Override
    public TargetPlatform createTargetPlatform(TargetPlatformConfigurationStub tpConfiguration,
            ExecutionEnvironmentConfiguration eeConfiguration, List<ReactorProject> reactorProjects,
            ReactorProject project) {
        return getImpl().createTargetPlatform(tpConfiguration, eeConfiguration, reactorProjects, project);
    }

    @Override
    public TargetPlatform createTargetPlatformWithUpdatedReactorContent(TargetPlatform baseTargetPlatform,
            List<?> upstreamProjectResults, PomDependencyCollector pomDependencies) {
        return getImpl().createTargetPlatformWithUpdatedReactorContent(baseTargetPlatform, upstreamProjectResults,
                pomDependencies);
    }

    private TargetPlatformFactory getImpl() {
        if (impl == null) {
            // TODO should be plexus-components!
            LocalMetadataRepository localMetadataRepo = getLocalMetadataRepository(mavenContext, localRepoIndices);
            LocalArtifactRepository localArtifactRepo = getLocalArtifactRepository(mavenContext, localRepoIndices);
            //TODO merge the impl here...
            impl = new TargetPlatformFactoryImpl(mavenContext, agent, localArtifactRepo, localMetadataRepo,
                    targetDefinitionResolverService, repositoryIdManager, projectManager, bundleResolver);
        }
        return impl;
    }

    private synchronized LocalMetadataRepository getLocalMetadataRepository(MavenContext context,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localMetadataRepository == null) {
            File localMavenRepoRoot = context.getLocalRepositoryRoot();
            RepositoryReader contentLocator = new LocalRepositoryReader(context);
            localMetadataRepository = new LocalMetadataRepository(getAgent(), localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), contentLocator);

        }
        return localMetadataRepository;
    }

    private synchronized LocalArtifactRepository getLocalArtifactRepository(MavenContext mavenContext,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localArtifactRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(mavenContext);
            localArtifactRepository = new LocalArtifactRepository(getAgent(), localRepoIndices, contentLocator);
        }
        return localArtifactRepository;
    }

    private IProvisioningAgent getAgent() {
        //force triggering service loads... just in case not initialized yet ...
        agent.getService(IArtifactRepositoryManager.class);
        return agent;
    }
}
