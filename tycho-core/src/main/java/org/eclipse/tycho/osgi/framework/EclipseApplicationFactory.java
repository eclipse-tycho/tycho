/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that resolves all the bundles that make up an Eclipse Application to run from a given
 * URI
 */
@Singleton
@Named
public class EclipseApplicationFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ToolchainManager toolchainManager;
    private final P2ResolverFactory resolverFactory;
    private final TargetPlatformFactory platformFactory;
    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public EclipseApplicationFactory(ToolchainManager toolchainManager,
                                     P2ResolverFactory resolverFactory,
                                     TargetPlatformFactory platformFactory,
                                     Provider<MavenSession> mavenSessionProvider) {
        this.toolchainManager = toolchainManager;
        this.resolverFactory = resolverFactory;
        this.platformFactory = platformFactory;
        this.mavenSessionProvider = mavenSessionProvider;
    }

    public EclipseApplication createEclipseApplication(MavenRepositoryLocation repositoryLocation, String name) {
        return createEclipseApplication(createTargetPlatform(List.of(repositoryLocation)), name);
    }

    public EclipseApplication createEclipseApplication(TargetPlatform targetPlatform, String name) {
        P2Resolver resolver = createResolver();
        EclipseApplication application = new EclipseApplication(name, resolver, targetPlatform, logger, mavenSessionProvider.get()
                .getAllProjects().stream().collect(Collectors.toMap(MavenProject::getBasedir, Function.identity())));
        //add the bare minimum required ...
        application.addBundle(Bundles.BUNDLE_CORE);
        application.addBundle(Bundles.BUNDLE_SCR);
        application.addBundle(Bundles.BUNDLE_APP);
        application.addBundle(Bundles.BUNDLE_LAUNCHER);
        return application;
    }

    public TargetPlatform createTargetPlatform(Collection<MavenRepositoryLocation> locations) {
        TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
        tpConfiguration.setIgnoreLocalArtifacts(true);
        tpConfiguration.setIncludeSourceMode(IncludeSourceMode.ignore);
        for (MavenRepositoryLocation location : locations) {
            tpConfiguration.addP2Repository(location);
        }
        int javaVersion = Runtime.version().feature();
        ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger, false,
                toolchainManager, mavenSessionProvider.get());
        eeConfiguration.setProfileConfiguration("JavaSE-" + javaVersion, "tycho-eclipse-application-resolver");
        return platformFactory.createTargetPlatform(tpConfiguration, eeConfiguration, null);
    }

    public P2Resolver createResolver() {
        return createResolver(List.of(TargetEnvironment.getRunningEnvironment()));
    }

    public P2Resolver createResolver(Collection<TargetEnvironment> environments) {
        P2Resolver resolver = resolverFactory.createResolver(environments);
        return resolver;
    }

}
