/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.test.util.ArtifactMock;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.test.util.ReactorProjectStub;
import org.eclipse.tycho.test.util.TestResolverFactory;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;

public class P2ResolverTestBase extends TychoPlexusTestCase {

    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_GROUP_ID = "test.groupId";

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private P2GeneratorImpl fullGenerator;
    private DefaultDependencyMetadataGenerator dependencyGenerator;

    protected TestResolverFactory resolverFactory;
    protected P2Resolver impl;
    protected TargetPlatformConfigurationStub tpConfig;
    protected PomDependencyCollectorImpl pomDependencies;
    protected List<ReactorProject> reactorProjects = new ArrayList<>();
    protected TargetPlatformFactoryImpl tpFactory;

    @Before
    final public void prepare() throws Exception {
        resolverFactory = new TestResolverFactory(logVerifier.getMavenLogger(), lookup(IProvisioningAgent.class),
                lookup(IRepositoryIdManager.class));
        MockMavenContext mavenContext = new MockMavenContext(null, logVerifier.getLogger());
        fullGenerator = new P2GeneratorImpl(true);
        fullGenerator.setMavenContext(mavenContext);
        BuildPropertiesParserForTesting buildPropertiesReader = new BuildPropertiesParserForTesting();
        fullGenerator.setBuildPropertiesParser(buildPropertiesReader);
        dependencyGenerator = new DefaultDependencyMetadataGenerator();
        dependencyGenerator.setBuildPropertiesParser(buildPropertiesReader);
        dependencyGenerator.setMavenContext(mavenContext);

        tpConfig = new TargetPlatformConfigurationStub();
        tpFactory = resolverFactory.getTargetPlatformFactoryImpl();
    }

    protected static List<TargetEnvironment> getEnvironments() {
        List<TargetEnvironment> environments = new ArrayList<>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        return environments;
    }

    protected final void addContextProject(File projectRoot, String packaging) throws IOException {
        ArtifactMock artifact = new ArtifactMock(projectRoot.getAbsoluteFile(), DEFAULT_GROUP_ID, projectRoot.getName(),
                DEFAULT_VERSION, packaging);

        DependencyMetadata metadata = fullGenerator.generateMetadata(artifact, getEnvironments(),
                new PublisherOptions());

        pomDependencies.addMavenArtifact(artifact, metadata.getInstallableUnits());
    }

    /**
     * Creates a {@link ReactorProject} instance to be added to the target platform and/or to be
     * used as project to be resolved.
     */
    protected final ReactorProject createReactorProject(File projectRoot, String packagingType, String artifactId) {
        OptionalResolutionAction optionalDependencies = OptionalResolutionAction.REQUIRE;
        return createReactorProject(projectRoot, packagingType, artifactId, optionalDependencies);
    }

    protected ReactorProject createReactorProject(File projectRoot, String packagingType, String artifactId,
            OptionalResolutionAction optionalDependencies) {
        ReactorProjectStub project = new ReactorProjectStub(projectRoot, DEFAULT_GROUP_ID, artifactId, DEFAULT_VERSION,
                packagingType);
        IDependencyMetadata metadata = dependencyGenerator.generateMetadata(new ArtifactMock(project, null),
                getEnvironments(), optionalDependencies, new PublisherOptions());
        project.setDependencyMetadata(metadata);
        return project;
    }

}
