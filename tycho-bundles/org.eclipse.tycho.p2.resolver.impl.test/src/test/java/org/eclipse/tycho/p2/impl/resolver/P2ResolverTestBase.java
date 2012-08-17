/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - apply DRY principle
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.impl.publisher.DefaultDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.repo.LocalRepositoryP2IndicesImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.remote.RemoteAgent;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.target.NoopEEResolverHints;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverService;
import org.eclipse.tycho.p2.target.TargetPlatformBuilderImpl;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.junit.Before;

public class P2ResolverTestBase {

    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    private static final String DEFAULT_GROUP_ID = "test.groupId";

    private P2GeneratorImpl fullGenerator;
    private DefaultDependencyMetadataGenerator dependencyGenerator;

    P2Resolver impl;
    TargetPlatformBuilderImpl context;

    @Before
    final public void prepare() {
        fullGenerator = new P2GeneratorImpl(true);
        BuildPropertiesParserForTesting buildPropertiesReader = new BuildPropertiesParserForTesting();
        fullGenerator.setBuildPropertiesParser(buildPropertiesReader);
        dependencyGenerator = new DefaultDependencyMetadataGenerator();
        dependencyGenerator.setBuildPropertiesParser(buildPropertiesReader);
    }

    static List<Map<String, String>> getEnvironments() {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        environments.add(newEnvironment("linux", "gtk", "x86_64"));

        return environments;
    }

    static Map<String, String> newEnvironment(String os, String ws, String arch) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("osgi.os", os);
        properties.put("osgi.ws", ws);
        properties.put("osgi.arch", arch);

        // TODO does not belong here
        properties.put("org.eclipse.update.install.features", "true");

        return properties;
    }

    final void addContextProject(File projectRoot, String packaging) throws IOException {
        ArtifactMock artifact = new ArtifactMock(projectRoot.getCanonicalFile(), DEFAULT_GROUP_ID,
                projectRoot.getName(), DEFAULT_VERSION, packaging);

        DependencyMetadata metadata = fullGenerator.generateMetadata(artifact, getEnvironments());

        context.addMavenArtifact(new ClassifiedLocation(artifact), artifact, metadata.getInstallableUnits());
    }

    final void addReactorProject(File projectRoot, String packagingType, String artifactId) {
        ArtifactMock artifact = new ArtifactMock(projectRoot, DEFAULT_GROUP_ID, artifactId, DEFAULT_VERSION,
                packagingType);
        IDependencyMetadata metadata = dependencyGenerator.generateMetadata(artifact, getEnvironments(),
                OptionalResolutionAction.REQUIRE);
        artifact.setDependencyMetadata(metadata);
        context.addReactorArtifact(artifact);
    }

    static File getLocalRepositoryLocation() throws IOException {
        return new File("target/localrepo").getCanonicalFile();
    }

    /**
     * Creates a target platform builder without any special handling for execution environments.
     */
    protected final TargetPlatformBuilderImpl createTargetPlatformBuilder() throws Exception {
        return new TestTargetPlatformBuilderFactory().createTargetPlatformBuilder(new NoopEEResolverHints());
    }

    protected final TargetPlatformBuilderImpl createTargetPlatformBuilderWithEE(String bree) throws Exception {
        return new TestTargetPlatformBuilderFactory().createTargetPlatformBuilder(bree);
    }

    private static class TestTargetPlatformBuilderFactory {

        private MavenContext mavenContext;
        private TargetDefinitionResolverService targetDefinitionResolverService;
        private LocalMetadataRepository localMetadataRepo;
        private LocalArtifactRepository localArtifactRepo;

        TestTargetPlatformBuilderFactory() throws Exception {
            boolean offline = false;
            mavenContext = createMavenContext(offline, new MavenLoggerStub());

            targetDefinitionResolverService = new TargetDefinitionResolverService(mavenContext);

            File localMavenRepoRoot = mavenContext.getLocalRepositoryRoot();
            LocalRepositoryP2Indices localRepoIndices = createLocalRepoIndices(mavenContext);
            LocalRepositoryReader localRepositoryReader = new LocalRepositoryReader(localMavenRepoRoot);
            localMetadataRepo = new LocalMetadataRepository(localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), localRepositoryReader);
            localArtifactRepo = new LocalArtifactRepository(localRepoIndices, localRepositoryReader);
        }

        public TargetPlatformBuilderImpl createTargetPlatformBuilder(
                ExecutionEnvironmentResolutionHints executionEnvironment) throws Exception {
            return new TargetPlatformBuilderImpl(new RemoteAgent(mavenContext), mavenContext,
                    targetDefinitionResolverService, executionEnvironment, localArtifactRepo, localMetadataRepo);
        }

        TargetPlatformBuilderImpl createTargetPlatformBuilder(String bree) throws Exception {
            return new TargetPlatformBuilderImpl(new RemoteAgent(mavenContext), mavenContext,
                    targetDefinitionResolverService, bree, localArtifactRepo, localMetadataRepo);
        }

        private MavenContext createMavenContext(boolean offline, MavenLogger logger) throws IOException {
            MavenContextImpl mavenContext = new MavenContextImpl();
            mavenContext.setOffline(offline);
            mavenContext.setLocalRepositoryRoot(getLocalRepositoryLocation());
            mavenContext.setLogger(logger);
            return mavenContext;
        }

        private LocalRepositoryP2Indices createLocalRepoIndices(MavenContext mavenContext) {
            LocalRepositoryP2IndicesImpl localRepoIndices = new LocalRepositoryP2IndicesImpl();
            localRepoIndices.setMavenContext(mavenContext);
            localRepoIndices.setFileLockService(new NoopFileLockService());
            return localRepoIndices;
        }
    }
}
