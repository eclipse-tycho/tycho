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
package org.eclipse.tycho.p2maven.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;

@SuppressWarnings("restriction")
@Component(role = org.eclipse.equinox.internal.p2.repository.Transport.class, hint = TychoRepositoryTransport.HINT)
public class TychoRepositoryTransport extends org.eclipse.equinox.internal.p2.repository.Transport
		implements Initializable {

	public static final String HINT = "tycho";

	private NumberFormat numberFormat = NumberFormat.getNumberInstance();

	private SharedHttpCacheStorage httpCache;
	private LongAdder requests = new LongAdder();
	private LongAdder indexRequests = new LongAdder();

	private IProxyService proxyService;

	@Requirement
	private Logger logger;

	@Requirement
	private LegacySupport context;

	@Requirement
	private ArtifactHandlerManager artifactHandlerManager;

	@Requirement
	private MavenRepositorySettings repositorySettings;

	@Requirement
	private IProvisioningAgent agent;

	private Function<URI, Credentials> credentialsProvider;

	@Override
	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
		if (startPos > 0) {
			return new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"range downloads are not implemented");
		}
		return download(toDownload, target, monitor);
	}

	@Override
	public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
		try {
			IOUtils.copy(stream(toDownload, monitor), target);
			return reportStatus(Status.OK_STATUS, target);
		} catch (AuthenticationFailedException e) {
			return new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"authentication failed for " + toDownload, e);
		} catch (IOException e) {
			return reportStatus(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"download from " + toDownload + " failed", e), target);
		} catch (CoreException e) {
			return reportStatus(e.getStatus(), target);
		}
	}

	private IStatus reportStatus(IStatus status, OutputStream target) {
		if (target instanceof IStateful) {
			IStateful stateful = (IStateful) target;
			stateful.setStatus(status);
		}
		return status;
	}

	@Override
	public synchronized InputStream stream(URI toDownload, IProgressMonitor monitor)
			throws FileNotFoundException, CoreException, AuthenticationFailedException {
		logger.debug("Request stream for " + toDownload + "...");
		requests.increment();
		if (toDownload.toASCIIString().endsWith("p2.index")) {
			indexRequests.increment();
		}
		try {
			File cachedFile = getCachedFile(toDownload);
			if (cachedFile != null) {
				logger.debug(" --> routed through http-cache ...");
				return new FileInputStream(cachedFile);
			}
			return toDownload.toURL().openStream();
		} catch (FileNotFoundException e) {
			logger.debug(" --> not found!");
			throw e;
		} catch (IOException e) {
			logger.debug(" --> generic error: " + e);
			throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"download from " + toDownload + " failed", e));
		} finally {
			logger.debug("Total number of requests: " + requests.longValue() + " (" + indexRequests.longValue()
					+ " for p2.index)");
		}
	}

	@Override
	public long getLastModified(URI toDownload, IProgressMonitor monitor)
			throws CoreException, FileNotFoundException, AuthenticationFailedException {
		// TODO P2 cache manager relies on this method to throw an exception to work
		// correctly
		try {
			if (isHttp(toDownload)) {
				return httpCache.getCacheEntry(toDownload, logger).getLastModified(proxyService, credentialsProvider);
			}
			URLConnection connection = toDownload.toURL().openConnection();
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			return lastModified;
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"download from " + toDownload + " failed", e));
		}
	}

	public SharedHttpCacheStorage getHttpCache() {

		return httpCache;
	}

	public File getCachedFile(URI remoteFile) throws IOException {

		if (isHttp(remoteFile)) {
			return httpCache.getCacheEntry(remoteFile, logger).getCacheFile(proxyService, credentialsProvider);
		}
		return null;
	}

	public static boolean isHttp(URI remoteFile) {
		String scheme = remoteFile.getScheme();
		return scheme != null && scheme.toLowerCase().startsWith("http");
	}

	@Override
	public void initialize() throws InitializationException {
		if (httpCache == null) {
			MavenSession session = context.getSession();
			if (session == null) {
				throw new InitializationException("can't aquire maven session!");
			}
			File localRepoRoot = new File(session.getLocalRepository().getBasedir());

			File cacheLocation = new File(localRepoRoot, ".cache/tycho");
			cacheLocation.mkdirs();
			logger.info("### Using TychoRepositoryTransport for remote P2 access ###");
			logger.info("    Cache location:         " + cacheLocation);
			boolean offline = session.isOffline();
			boolean update = session.getRequest().isUpdateSnapshots();
			logger.info("    Transport mode:         " + (offline ? "offline" : "online"));
			logger.info("    Update mode:            " + (update ? "forced" : "cache first"));
			logger.info("    Minimum cache duration: " + SharedHttpCacheStorage.MIN_CACHE_PERIOD + " minutes");
			logger.info(
					"      (you can configure this with -Dtycho.p2.transport.min-cache-minutes=<desired minimum cache duration>)");
			numberFormat.setMaximumFractionDigits(2);
			httpCache = SharedHttpCacheStorage.getStorage(cacheLocation, offline, update);
			List<MavenRepositoryLocation> repositoryLocations = session.getProjects().stream()
					.map(MavenProject::getRemoteArtifactRepositories).flatMap(Collection::stream)
					.filter(r -> r.getLayout() instanceof P2ArtifactRepositoryLayout).map(r -> {
						try {
							return new MavenRepositoryLocation(r.getId(), new URL(r.getUrl()).toURI());
						} catch (MalformedURLException | URISyntaxException e) {
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
			credentialsProvider = uri -> {
				IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
				Stream<MavenRepositoryLocation> locations = repositoryLocations.stream();
				locations = Stream.concat(locations, repositoryIdManager.getKnownMavenRepositoryLocations());
				String requestUri = uri.normalize().toASCIIString();
				return locations.sorted((loc1, loc2) -> {
					// we wan't the longest prefix match, so first sort all uris by their length ...
					String s1 = loc1.getURL().normalize().toASCIIString();
					String s2 = loc2.getURL().normalize().toASCIIString();
					return Long.compare(s2.length(), s1.length());
				}).filter(loc -> {
					String prefix = loc.getURL().normalize().toASCIIString();
					return requestUri.startsWith(prefix);
				}).map(repositorySettings::getCredentials).filter(Objects::nonNull).findFirst().orElse(null);
			};
		}
	}

}
