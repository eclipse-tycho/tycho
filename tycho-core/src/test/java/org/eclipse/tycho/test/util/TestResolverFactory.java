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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.eclipse.tycho.MavenDependencyDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.DependencyResolutionException;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MavenModelFacade;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
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

    public TestResolverFactory(MavenLogger logger, IProvisioningAgent agent) {
        this.agent = agent;
        this.idManager = agent.getService(IRepositoryIdManager.class);
        boolean offline = false;
        mavenContext = createMavenContext(offline, logger);

        targetDefinitionResolverService = new TargetDefinitionResolverService();
        targetDefinitionResolverService.setMavenContext(mavenContext);
        targetDefinitionResolverService.setMavenDependenciesResolver(new MavenDependenciesResolver() {

            @Override
            public Collection<?> resolve(String groupId, String artifactId, String version, String packaging,
                    String classifier, Collection<String> dependencyScopes, int depth,
                    Collection<MavenArtifactRepositoryReference> additionalRepositories, Object session)
                    throws DependencyResolutionException {
                GAV gav = new GAV(groupId, artifactId, version);
                String relativePath = RepositoryLayoutHelper.getRelativePath(gav, null, "jar");
                // This is supposed to mimic Maven repo returning an artifact
                File file = new File(getLocalRepositoryLocation(), relativePath);
                try {
                    file.getParentFile().mkdirs();
                    File resourceFile = ResourceUtil.resourceFile("targetresolver/stubMavenRepo/" + relativePath);
                    Files.copy(resourceFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new DependencyResolutionException(e.getMessage(), List.of(e));
                }
                return List.of(new ArtifactMock(file, groupId, artifactId, version, "jar"));
            }

            @Override
            public MavenModelFacade loadModel(File modelFile) throws IOException {
                return null;
            }

            @Override
            public File getRepositoryRoot() {
                return mavenContext.getLocalRepositoryRoot();
            }
        });

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
        return new PomDependencyCollectorImpl(
                new MockMavenContext(mavenContext.getLocalRepositoryRoot(), mavenContext.getLogger()), project, agent);
    }

    public PomDependencyCollectorImpl newPomDependencyCollector() {
        return newPomDependencyCollector(new ReactorProjectStub(new File("."), "test"));
    }

    @Override
    public TargetPlatformFactory getTargetPlatformFactory() {
        return getTargetPlatformFactoryImpl();
    }

    public TargetPlatformFactoryImpl getTargetPlatformFactoryImpl() {
        return new TargetPlatformFactoryImpl(mavenContext, agent, localArtifactRepo, localMetadataRepo,
                targetDefinitionResolverService, idManager);
    }

    @Override
    public P2Resolver createResolver(MavenLogger logger) {
        return new P2ResolverImpl(getTargetPlatformFactoryImpl(), null, mavenContext.getLogger());
    }

    @Override
    public MavenDependencyDescriptor resolveDependencyDescriptor(ArtifactDescriptor artifactDescriptor) {
        return null;
    }
}
