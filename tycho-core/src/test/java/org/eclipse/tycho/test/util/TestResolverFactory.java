/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.MavenTargetLocationFactory;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2resolver.DefaultTargetDefinitionVariableResolver;
import org.eclipse.tycho.p2resolver.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2resolver.P2ResolverImpl;
import org.eclipse.tycho.p2resolver.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolverService;
import org.eclipse.tycho.p2resolver.TargetPlatformFactoryImpl;

public class TestResolverFactory implements P2ResolverFactory {

    public final MavenContext mavenContext;
    private TargetDefinitionResolverService targetDefinitionResolverService;
    private LocalMetadataRepository localMetadataRepo;
    private LocalArtifactRepository localArtifactRepo;
    private IProvisioningAgent agent;
    private IRepositoryIdManager idManager;
    private Logger logger2;

    public TestResolverFactory(MavenLogger logger, Logger logger2, IProvisioningAgent agent,
            MavenTargetLocationFactory resolve) {
        this.logger2 = logger2;
        this.agent = agent;
        this.idManager = agent.getService(IRepositoryIdManager.class);
        boolean offline = false;
        mavenContext = createMavenContext(offline, logger);

        targetDefinitionResolverService = new TargetDefinitionResolverService();
        targetDefinitionResolverService.setMavenContext(mavenContext);
        targetDefinitionResolverService.setMavenDependenciesResolver(resolve);
        targetDefinitionResolverService.setTargetDefinitionVariableResolver(
                new DefaultTargetDefinitionVariableResolver(mavenContext, logger2));

        File localMavenRepoRoot = mavenContext.getLocalRepositoryRoot();
        LocalRepositoryP2Indices localRepoIndices = createLocalRepoIndices(mavenContext);
        LocalRepositoryReader localRepositoryReader = new LocalRepositoryReader(mavenContext);
        localMetadataRepo = new LocalMetadataRepository(agent, localMavenRepoRoot.toURI(),
                localRepoIndices.getMetadataIndex(), localRepositoryReader);
        localArtifactRepo = new LocalArtifactRepository(agent, localRepoIndices, localRepositoryReader);
    }

    public LocalMetadataRepository getLocalMetadataRepository() {
        return localMetadataRepo;
    }

    private MavenContext createMavenContext(boolean offline, MavenLogger logger) {
        return new MockMavenContext(getLocalRepositoryLocation(), offline, logger, new Properties());
    }

    // TODO use TemporaryLocalMavenRepository
    static File getLocalRepositoryLocation() {
        return new File("target/localrepo").getAbsoluteFile();
    }

    private LocalRepositoryP2Indices createLocalRepoIndices(MavenContext mavenContext) {
        LocalRepositoryP2IndicesImpl localRepoIndices = new LocalRepositoryP2IndicesImpl();
        localRepoIndices.setMavenContext(mavenContext);
        localRepoIndices.setFileLockService(new NoopFileLockService());
        return localRepoIndices;
    }

    @Override
    public PomDependencyCollectorImpl newPomDependencyCollector(ReactorProject project) {
        return new PomDependencyCollectorImpl(logger2, project, agent);
    }

    public PomDependencyCollectorImpl newPomDependencyCollector() {
        return newPomDependencyCollector(new ReactorProjectStub(new File("."), "test"));
    }

    public TargetPlatformFactoryImpl getTargetPlatformFactoryImpl() {
        return new TargetPlatformFactoryImpl(mavenContext, agent, localArtifactRepo, localMetadataRepo,
                targetDefinitionResolverService, idManager, null, null);
    }

    @Override
    public P2Resolver createResolver(Collection<TargetEnvironment> environments) {
        return new P2ResolverImpl(getTargetPlatformFactoryImpl(), null, mavenContext.getLogger(), environments);
    }

    @Override
    public MavenDependencyDescriptor resolveDependencyDescriptor(ArtifactDescriptor artifactDescriptor) {
        return null;
    }

}
