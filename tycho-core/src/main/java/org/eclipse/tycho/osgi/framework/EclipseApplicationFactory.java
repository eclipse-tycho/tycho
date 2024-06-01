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

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
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

/**
 * Component that resolves all the bundles that make up an Eclipse Application to run from a given
 * URI
 */
@Component(role = EclipseApplicationFactory.class)
@SessionScoped
public class EclipseApplicationFactory {

    @Requirement
    private ToolchainManager toolchainManager;

    @Requirement
    private P2ResolverFactory resolverFactory;

    @Requirement
    private TargetPlatformFactory platformFactory;

    @Requirement
    private Logger logger;

    private MavenSession mavenSession;

    @Inject
    public EclipseApplicationFactory(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    public EclipseApplication createEclipseApplication(MavenRepositoryLocation repositoryLocation, String name) {
        return createEclipseApplication(createTargetPlatform(List.of(repositoryLocation)), name);
    }

    public EclipseApplication createEclipseApplication(TargetPlatform targetPlatform, String name) {
        P2Resolver resolver = createResolver();
        EclipseApplication application = new EclipseApplication(name, resolver, targetPlatform, logger, mavenSession
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
                toolchainManager, mavenSession);
        eeConfiguration.setProfileConfiguration("JavaSE-" + javaVersion, "tycho-eclipse-application-resolver");
        TargetPlatform targetPlatform = platformFactory.createTargetPlatform(tpConfiguration, eeConfiguration, null);
        return targetPlatform;
    }

    public P2Resolver createResolver() {
        return createResolver(List.of(TargetEnvironment.getRunningEnvironment()));
    }

    public P2Resolver createResolver(Collection<TargetEnvironment> environments) {
        P2Resolver resolver = resolverFactory.createResolver(environments);
        return resolver;
    }

}
