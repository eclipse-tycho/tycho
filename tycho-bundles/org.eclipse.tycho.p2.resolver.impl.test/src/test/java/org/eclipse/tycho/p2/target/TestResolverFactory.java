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
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.Properties;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenContextImpl;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.remote.RemoteAgent;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.resolver.P2ResolverImpl;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;
import org.eclipse.tycho.repository.local.index.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.test.util.NoopFileLockService;

public class TestResolverFactory implements P2ResolverFactory {

    private MavenContext mavenContext;
    private TargetDefinitionResolverService targetDefinitionResolverService;
    private LocalMetadataRepository localMetadataRepo;
    private LocalArtifactRepository localArtifactRepo;

    public TestResolverFactory(MavenLogger logger) {
        boolean offline = false;
        mavenContext = createMavenContext(offline, logger);

        targetDefinitionResolverService = new TargetDefinitionResolverService();
        targetDefinitionResolverService.setMavenContext(mavenContext);

        File localMavenRepoRoot = mavenContext.getLocalRepositoryRoot();
        LocalRepositoryP2Indices localRepoIndices = createLocalRepoIndices(mavenContext);
        LocalRepositoryReader localRepositoryReader = new LocalRepositoryReader(localMavenRepoRoot);
        localMetadataRepo = new LocalMetadataRepository(localMavenRepoRoot.toURI(), localRepoIndices.getMetadataIndex(),
                localRepositoryReader);
        localArtifactRepo = new LocalArtifactRepository(localRepoIndices, localRepositoryReader);
    }

    public LocalMetadataRepository getLocalMetadataRepository() {
        return localMetadataRepo;
    }

    private MavenContext createMavenContext(boolean offline, MavenLogger logger) {
        return new MavenContextImpl(getLocalRepositoryLocation(), offline, logger, new Properties());
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
        return new PomDependencyCollectorImpl(
                new MavenContextImpl(mavenContext.getLocalRepositoryRoot(), mavenContext.getLogger()), project);
    }

    public PomDependencyCollectorImpl newPomDependencyCollector() {
        return newPomDependencyCollector(new ReactorProjectStub(new File("."), "test"));
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return getTargetPlatformFactoryImpl();
    }

    public TargetPlatformFactoryImpl getTargetPlatformFactoryImpl() {
        try {
            return new TargetPlatformFactoryImpl(mavenContext, new RemoteAgent(mavenContext), localArtifactRepo,
                    localMetadataRepo, targetDefinitionResolverService);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public P2Resolver createResolver(MavenLogger logger) {
        return new P2ResolverImpl(getTargetPlatformFactoryImpl(), mavenContext.getLogger());
    }
}
