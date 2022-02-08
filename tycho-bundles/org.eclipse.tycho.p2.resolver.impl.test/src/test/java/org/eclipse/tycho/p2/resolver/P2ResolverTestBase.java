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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DefaultDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.target.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2.target.TargetPlatformFactoryImpl;
import org.eclipse.tycho.p2.target.TestResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;

public class P2ResolverTestBase {

    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_GROUP_ID = "test.groupId";

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private P2GeneratorImpl fullGenerator;
    private DefaultDependencyMetadataGenerator dependencyGenerator;

    protected TestResolverFactory resolverFactory = new TestResolverFactory(logVerifier.getLogger());
    protected P2Resolver impl;
    protected TargetPlatformConfigurationStub tpConfig;
    protected PomDependencyCollectorImpl pomDependencies;
    protected List<ReactorProject> reactorProjects = new ArrayList<>();
    protected TargetPlatformFactoryImpl tpFactory;

    @Before
    final public void prepare() throws Exception {
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
