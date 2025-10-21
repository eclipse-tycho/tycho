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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;

@Named
@SessionScoped
public class DefaultEclipseApplicationFactory implements EclipseApplicationFactory {

    @Inject
    private ToolchainManager toolchainManager;

    @Inject
    private P2ResolverFactory resolverFactory;

    @Inject
    private TargetPlatformFactory platformFactory;

    @Inject
    private Logger logger;

    private MavenSession mavenSession;

    @Inject
    public DefaultEclipseApplicationFactory(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    @Override
    public EclipseApplication createEclipseApplication(MavenRepositoryLocation repositoryLocation, String name) {
        return createEclipseApplication(createTargetPlatform(List.of(repositoryLocation)), name);
    }

    @Override
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

    @Override
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

    @Override
    public Collection<Path> getApiBaselineBundles(Collection<MavenRepositoryLocation> baselineRepoLocations,
            ArtifactKey artifactKey, Collection<TargetEnvironment> environment)
            throws IllegalArtifactReferenceException {
        P2Resolver resolver = createResolver(environment);
        resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, artifactKey.getId(), "0.0.0");
        List<Path> resolvedBundles = new ArrayList<>();
        TargetPlatform targetPlatform = createTargetPlatform(baselineRepoLocations);
        for (P2ResolutionResult result : resolver.resolveTargetDependencies(targetPlatform, null).values()) {
            for (Entry entry : result.getArtifacts()) {
                if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(entry.getType())
                        && !"org.eclipse.osgi".equals(entry.getId())) {
                    File location = entry.getLocation(true);
                    if (location == null) {
                        logger.warn("Cannot resolve location for bundle " + entry.getId() + " " + entry.getVersion()
                                + ". The artifact may not be available or failed to download.");
                        continue;
                    }
                    resolvedBundles.add(location.toPath());
                }
            }
        }
        return resolvedBundles;
    }

    @Override
    public P2Resolver createResolver() {
        return createResolver(List.of(TargetEnvironment.getRunningEnvironment()));
    }

    @Override
    public P2Resolver createResolver(Collection<TargetEnvironment> environments) {
        P2Resolver resolver = resolverFactory.createResolver(environments);
        return resolver;
    }

}
