/*******************************************************************************
 * Copyright (c) 2023, 2024 Christoph Läubrich and others.
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.osgi.framework.Bundles;
import org.eclipse.tycho.osgi.framework.EclipseApplication;
import org.eclipse.tycho.osgi.framework.EclipseApplicationFactory;
import org.eclipse.tycho.osgi.framework.EclipseApplicationManager;
import org.eclipse.tycho.osgi.framework.Features;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogEntry;
/**
 * Component that resolves the bundles that make up the ApiApplication from a
 * given URI
 */
@Named
@Singleton
public class ApiApplicationResolver {

	@Inject
	private EclipseApplicationFactory applicationFactory;

	@Inject
	private EclipseApplicationManager applicationManager;

	public Collection<Path> getApiBaselineBundles(Collection<MavenRepositoryLocation> baselineRepoLocations,
			ArtifactKey artifactKey, Collection<TargetEnvironment> environment)
			throws IllegalArtifactReferenceException {
		P2Resolver resolver = applicationFactory.createResolver(environment);
		resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, artifactKey.getId(), "0.0.0");
		List<Path> resolvedBundles = new ArrayList<>();
		TargetPlatform targetPlatform = applicationFactory.createTargetPlatform(baselineRepoLocations);
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

	public EclipseApplication getApiApplication(MavenRepositoryLocation apiToolsRepo) {

		EclipseApplication application = applicationManager.getApplication(apiToolsRepo, new Bundles(Set.of(Bundles.BUNDLE_API_TOOLS)),
				new Features(Set.of()), "Api Tools");
		application.setLoggingFilter(ApiApplicationResolver::isOnlyDebug);
		return application;
	}

	private static boolean isOnlyDebug(LogEntry entry) {
		String message = entry.getMessage();
		if (message.contains("The workspace ") && message.contains("with unsaved changes")) {
			return true;
		}
		if (message.contains("Workspace was not properly initialized or has already shutdown")) {
			return true;
		}
		if (message.contains("Platform proxy API not available")) {
			return true;
		}
		if (message.contains("Error processing mirrors URL")) {
			return true;
		}
		if (entry.getException() instanceof BundleException) {
			return true;
		}
		return false;
	}

}
