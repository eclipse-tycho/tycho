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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
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
@Singleton
@Named("MavenURLStreamHandlerService")
public class MavenURLStreamHandlerService extends AbstractURLStreamHandlerService implements EquinoxLifecycleListener {

	private static final String PROTOCOL = "mvn";

	@Inject
	private Logger logger;

	@Inject
	private LegacySupport context;

	@Inject
	private RepositorySystem repositorySystem;

	private MavenSession mavenSession;

	@Override
	public void afterFrameworkStarted(EmbeddedEquinox framework) {
		this.mavenSession = context.getSession();
		framework.registerService(URLStreamHandlerService.class, this,
				Map.of(URLConstants.URL_HANDLER_PROTOCOL, new String[] { PROTOCOL }));
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
					artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
				} else {
					artifact = new DefaultArtifact(groupId, artifactId, "", type, version);
				}
				logger.debug("Resolving " + artifact);
				ArtifactRequest artifactRequest = new ArtifactRequest();
				artifactRequest.setArtifact(artifact);
				artifactRequest.addRepository(RepositoryUtils.toRepo(mavenSession.getLocalRepository()));
				for (ArtifactRepository repo : mavenSession.getCurrentProject().getRemoteArtifactRepositories()) {
					artifactRequest.addRepository(RepositoryUtils.toRepo(repo));
				}
				try {
					ArtifactResult artifactResult = repositorySystem
							.resolveArtifact(mavenSession.getRepositorySession(), artifactRequest);
					artifact = artifactResult.getArtifact();
					if (!artifactResult.getExceptions().isEmpty()) {
						String message = "Resolving " + artifact + " failed";
						List<Exception> exceptions = artifactResult.getExceptions();
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
					if (artifactResult.isMissing()) {
						throw new IOException("artifact " + Arrays.toString(coordinates)
								+ " could not be retrieved from any of the available repositories");
					}
				} catch (ArtifactResolutionException e) {
					throw new IOException("artifact " + Arrays.toString(coordinates)
							+ " could not be resolved");
				}
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

		@Override
		public long getLastModified() {
			try {
				connect();
			} catch (IOException e) {
				return 0;
			}
			return artifact.getFile().lastModified();
		}

	}

}
