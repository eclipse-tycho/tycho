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
package org.eclipse.tycho.apitools;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.core.resolver.TargetPlatformConfigurationException;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

/**
 * Component that resolves the bundles that make up the ApiApplication from a
 * given URI
 */
@Component(role = ApiApplicationResolver.class)
@SessionScoped
public class ApiApplicationResolver {

	private final Map<URI, Collection<Path>> cache = new ConcurrentHashMap<>();

	@Requirement
	private ToolchainManager toolchainManager;

	@Requirement
	private P2ResolverFactory resolverFactory;

	@Requirement
	private Logger logger;

	private MavenSession mavenSession;

	@Inject
	public ApiApplicationResolver(MavenSession mavenSession) {
		this.mavenSession = mavenSession;
	}

	public Collection<Path> getApiBaselineBundles(Collection<MavenRepositoryLocation> baselineRepoLocations,
			ArtifactKey artifactKey)
			throws IllegalArtifactReferenceException {
		P2Resolver resolver = createResolver();
		resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, artifactKey.getId(), "0.0.0");
		return resolveBundles(resolver, baselineRepoLocations);
	}

	public Collection<Path> getApiApplicationBundles(MavenRepositoryLocation apiToolsRepo) {
		return cache.computeIfAbsent(apiToolsRepo.getURL().normalize(), x -> {
			logger.info("Resolve API tools runtime from " + apiToolsRepo + "...");
			P2Resolver resolver = createResolver();
			try {
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.pde.api.tools", "0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "javax.annotation", "0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.equinox.p2.transport.ecf",
						"0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.ecf.provider.filetransfer.ssl",
						"0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.equinox.p2.touchpoint.natives",
						"0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.osgi.compatibility.state",
						"0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.core.runtime", "0.0.0");
				resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, "org.eclipse.equinox.launcher", "0.0.0");

			} catch (IllegalArtifactReferenceException e) {
				throw new TargetPlatformConfigurationException("Can't add API tools requirement", e);
			}
			List<MavenRepositoryLocation> locations = List.of(apiToolsRepo);
			List<Path> resolvedBundles = resolveBundles(resolver, locations);
			logger.debug("API Runtime resolved with " + resolvedBundles.size() + " bundles.");
			return resolvedBundles;
		});
	}

	private List<Path> resolveBundles(P2Resolver resolver, Collection<MavenRepositoryLocation> locations) {
		List<Path> resolvedBundles = new ArrayList<>();
		TargetPlatform targetPlatform = createTargetPlatform(locations);
		for (P2ResolutionResult result : resolver.resolveTargetDependencies(targetPlatform, null).values()) {
			for (Entry entry : result.getArtifacts()) {
				if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(entry.getType())
						&& !"org.eclipse.osgi".equals(entry.getId())) {
					resolvedBundles.add(entry.getLocation(true).toPath());
				}
			}
		}
		return resolvedBundles;
	}

	private TargetPlatform createTargetPlatform(Collection<MavenRepositoryLocation> locations) {
		TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
		tpConfiguration.setIgnoreLocalArtifacts(true);
		tpConfiguration.setIncludeSourceMode(IncludeSourceMode.ignore);
		for (MavenRepositoryLocation location : locations) {
			tpConfiguration.addP2Repository(location);
		}
		int javaVersion = Runtime.version().feature();
		ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl(logger, false,
				toolchainManager, mavenSession);
		eeConfiguration.setProfileConfiguration("JavaSE-" + javaVersion, "tycho-api-tools ApiApplicationResolver");
		TargetPlatform targetPlatform = resolverFactory.getTargetPlatformFactory().createTargetPlatform(tpConfiguration,
				eeConfiguration, null);
		return targetPlatform;
	}

	private P2Resolver createResolver() {
		P2Resolver resolver = resolverFactory
				.createResolver(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
		return resolver;
	}

}
