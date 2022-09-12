/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * 
 * This makes the <code>mvn</code> protocol available to P2 e.g. to load
 * updates-sites.
 */
@Component(role = EquinoxLifecycleListener.class, hint = "MavenURLStreamHandlerService")
public class MavenURLStreamHandlerService extends AbstractURLStreamHandlerService implements EquinoxLifecycleListener {

	private static final String PROTOCOL = "mvn";

	@Requirement
	private Logger logger;

	@Requirement
	private LegacySupport context;

	@Requirement
	private RepositorySystem repositorySystem;

	private MavenSession mavenSession;

	@Override
	public void afterFrameworkStarted(EmbeddedEquinox framework) {
		this.mavenSession = context.getSession();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { PROTOCOL });
		framework.registerService(URLStreamHandlerService.class, this, properties);
	}

	@Override
	public URLConnection openConnection(URL url) throws IOException {
		MavenSession session = context.getSession();
		if (session == null) {
			logger.warn(
					"Called connect() outside maven thread, using global session, project specific repositories or configuration might be ignored!");
			session = mavenSession;
		}
		return new MavenURLConnection(url, session, repositorySystem, logger);
	}

	private static final class MavenURLConnection extends URLConnection {

		private String subPath;

		private Artifact artifact;

		private Logger logger;

		private MavenSession mavenSession;

		private RepositorySystem repositorySystem;

		protected MavenURLConnection(URL url, MavenSession mavenSession, RepositorySystem repositorySystem,
				Logger logger) {
			super(url);
			this.mavenSession = mavenSession;
			this.repositorySystem = repositorySystem;
			this.logger = logger;
		}

		@Override
		public void connect() throws IOException {
			try {
				if (artifact != null) {
					return;
				}
				String path = url.getPath();
				if (path == null) {
					throw new IOException("maven coordinates are missing");
				}
				int subPathIndex = path.indexOf('/');
				String[] coordinates;
				if (subPathIndex > -1) {
					subPath = path.substring(subPathIndex);
					coordinates = path.substring(0, subPathIndex).split(":");
				} else {
					coordinates = path.split(":");
				}
				if (coordinates.length < 3) {
					throw new IOException("required format is groupId:artifactId:version[:packaging[:classifier]]");
				}
				String type = coordinates.length > 3 ? coordinates[3] : "jar";
				String classifier = coordinates.length > 4 ? coordinates[4] : null;
				String groupId = coordinates[0];
				String artifactId = coordinates[1];
				String version = coordinates[2];
				if (classifier != null && !classifier.isEmpty()) {
					artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, type,
							classifier);
				} else {
					artifact = repositorySystem.createArtifact(groupId, artifactId, version, null, type);
				}
				logger.debug("Resolve " + artifact + "...");
				ArtifactResolutionRequest request = new ArtifactResolutionRequest();
				request.setArtifact(artifact);
				request.setResolveRoot(true);
				request.setOffline(mavenSession.isOffline());
				request.setLocalRepository(mavenSession.getLocalRepository());
				request.setResolveTransitively(false);
				request.setRemoteRepositories(mavenSession.getCurrentProject().getRemoteArtifactRepositories());
				ArtifactResolutionResult result = repositorySystem.resolve(request);
				if (result.hasExceptions()) {
					String message = "resolving " + artifact + " failed!";
					List<Exception> exceptions = result.getExceptions();
					if (exceptions.size() == 1) {
						throw new IOException(message, exceptions.get(0));
					} else {
						IOException exception = new IOException(message);
						for (Exception suppressed : exceptions) {
							exception.addSuppressed(suppressed);
						}
						throw exception;
					}
				}
				Set<Artifact> artifacts = result.getArtifacts();
				if (artifacts.isEmpty()) {
					throw new IOException("artifact " + Arrays.toString(coordinates)
							+ " could not be retrieved from any of the available repositories");
				}
				if (artifacts.size() > 1) {
					throw new IOException(
							"artifact " + Arrays.toString(coordinates) + " resolves to multiple artifacts");
				}
				artifact = artifacts.iterator().next();
			} catch (RuntimeException e) {
				throw new IOException("internal error connecting to maven url " + url, e);
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			connect();
			File location = artifact.getFile();
			if (subPath == null) {
				return new FileInputStream(location);
			}
			String urlSpec = "jar:" + location.toURI() + "!" + subPath;
			return new URL(urlSpec).openStream();
		}

	}

}
