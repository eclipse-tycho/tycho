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
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
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
    public void prepare() {
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

    void addContextProject(File projectRoot, String packaging) throws IOException {
        ArtifactMock artifact = new ArtifactMock(projectRoot.getCanonicalFile(), DEFAULT_GROUP_ID,
                projectRoot.getName(), DEFAULT_VERSION, packaging);

        DependencyMetadata metadata = fullGenerator.generateMetadata(artifact, getEnvironments());

        context.addMavenArtifact(new ClassifiedLocation(artifact), artifact, metadata.getInstallableUnits());
    }

    void addReactorProject(File projectRoot, String packagingType, String artifactId) {
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

    protected MavenContext createMavenContext(boolean offline, MavenLogger logger) throws IOException {
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setOffline(offline);
        mavenContext.setLocalRepositoryRoot(getLocalRepositoryLocation());
        mavenContext.setLogger(logger);
        return mavenContext;
    }

    protected P2ResolverFactoryImpl createP2ResolverFactory(boolean offline) throws IOException {
        P2ResolverFactoryImpl p2ResolverFactory = new P2ResolverFactoryImpl();
        MavenContext mavenContext = createMavenContext(offline, new MavenLoggerStub());
        p2ResolverFactory.setMavenContext(mavenContext);
        p2ResolverFactory.setLocalRepositoryIndices(createLocalRepoIndices(mavenContext));
        p2ResolverFactory.setRemoteAgentManager(createRemoteAgentManager(mavenContext));
        p2ResolverFactory.setTargetDefinitionResolverService(new TargetDefinitionResolverService(mavenContext));
        return p2ResolverFactory;
    }

    protected LocalRepositoryP2Indices createLocalRepoIndices(MavenContext mavenContext) {
        LocalRepositoryP2IndicesImpl localRepoIndices = new LocalRepositoryP2IndicesImpl();
        localRepoIndices.setMavenContext(mavenContext);
        localRepoIndices.setFileLockService(new NoopFileLockService());
        return localRepoIndices;
    }

    protected RemoteAgentManager createRemoteAgentManager(MavenContext mavenContext) {
        RemoteAgentManager manager = new RemoteAgentManager();
        manager.setMavenContext(mavenContext);
        return manager;
    }
}
