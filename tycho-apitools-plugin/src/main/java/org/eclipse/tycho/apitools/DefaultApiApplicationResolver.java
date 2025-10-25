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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TargetEnvironment;
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
public class DefaultApiApplicationResolver implements ApiApplicationResolver {

	@Inject
	private Logger logger;

	@Inject
	private EclipseApplicationFactory applicationFactory;

	@Inject
	private EclipseApplicationManager applicationManager;

	public Collection<Path> getApiBaselineBundles(Collection<MavenRepositoryLocation> baselineRepoLocations,
			ArtifactKey artifactKey, Collection<TargetEnvironment> environment)
			throws IllegalArtifactReferenceException {
		return applicationFactory.getApiBaselineBundles(baselineRepoLocations, artifactKey, environment);
	}

	public EclipseApplication getApiApplication(MavenRepositoryLocation apiToolsRepo) {

		EclipseApplication application = applicationManager.getApplication(apiToolsRepo, new Bundles(Set.of(Bundles.BUNDLE_API_TOOLS)),
				new Features(Set.of()), "Api Tools");
		application.setLoggingFilter(DefaultApiApplicationResolver::isOnlyDebug);
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
