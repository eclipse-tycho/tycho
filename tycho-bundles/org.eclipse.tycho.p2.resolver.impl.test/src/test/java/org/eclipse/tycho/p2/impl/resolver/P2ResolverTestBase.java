/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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

import static org.eclipse.tycho.p2.impl.resolver.P2ResolverTest.getLocalRepositoryLocation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.MavenContextImpl;
import org.eclipse.tycho.p2.impl.publisher.DefaultDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.junit.After;

public class P2ResolverTestBase {

    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    private static final String DEFAULT_GROUP_ID = "test.groupId";

    private final P2GeneratorImpl fullGenerator = new P2GeneratorImpl(true);
    private final DependencyMetadataGenerator dependencyGenerator = new DefaultDependencyMetadataGenerator();

    P2Resolver impl;
    ResolutionContextImpl context;

    @After
    public void stopResolver() {
        context.stop();
    }

    static List<Map<String, String>> getEnvironments() {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("osgi.os", "linux");
        properties.put("osgi.ws", "gtk");
        properties.put("osgi.arch", "x86_64");

        // TODO does not belong here
        properties.put("org.eclipse.update.install.features", "true");

        environments.add(properties);

        return environments;
    }

    void addContextProject(File projectRoot, String packaging) throws IOException {
        ArtifactMock artifact = new ArtifactMock(projectRoot.getCanonicalFile(), DEFAULT_GROUP_ID,
                projectRoot.getName(), DEFAULT_VERSION, packaging);

        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        fullGenerator.generateMetadata(artifact, getEnvironments(), units, null);

        context.addMavenArtifact(new ClassifiedLocation(artifact), artifact, units);
    }

    void addReactorProject(File projectRoot, String packagingType, String artifactId) {
        ArtifactMock artifact = new ArtifactMock(projectRoot, DEFAULT_GROUP_ID, artifactId, DEFAULT_VERSION,
                packagingType);
        artifact.setDependencyMetadata(dependencyGenerator.generateMetadata(artifact, getEnvironments()));
        context.addReactorArtifact(artifact);
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
        p2ResolverFactory.setMavenContext(createMavenContext(offline, new MavenLoggerStub()));
        return p2ResolverFactory;
    }

}
