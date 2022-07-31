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
package org.eclipse.tycho.p2maven.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;
import org.osgi.framework.Constants;

@Component(role = EquinoxLifecycleListener.class, hint = "TychoP2Transport")
public class TychoRepositoryTransport extends org.eclipse.equinox.internal.p2.repository.Transport
		implements IAgentServiceFactory, EquinoxLifecycleListener {

	static final boolean DEBUG = Boolean.getBoolean("tycho.p2.transport.debug");

	private NumberFormat numberFormat = NumberFormat.getNumberInstance();

	private SharedHttpCacheStorage httpCache;
	private LongAdder requests = new LongAdder();
	private LongAdder indexRequests = new LongAdder();

	private Function<URI, Credentials> credentialsProvider;

	@Requirement
	private Logger logger;

	@Requirement
	private LegacySupport legacySupport;

	private Supplier<IProxyService> proxyService;

	public TychoRepositoryTransport() {
//	credentialsProvider = uri -> {
//        if (mavenRepositorySettings == null) {
//            return null;
//        }
//        IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
//        Stream<MavenRepositoryLocation> locations = mavenContext.getMavenRepositoryLocations();
//        if (repositoryIdManager instanceof RemoteRepositoryLoadingHelper) {
//            RemoteRepositoryLoadingHelper repositoryLoadingHelper = (RemoteRepositoryLoadingHelper) repositoryIdManager;
//            locations = Stream.concat(locations,
//                    repositoryLoadingHelper.getKnownMavenRepositoryLocations());
//        }
//        String requestUri = uri.normalize().toASCIIString();
//        return locations.sorted((loc1, loc2) -> {
//            //we wan't the longest prefix match, so first sort all uris by their length ...
//            String s1 = loc1.getURL().normalize().toASCIIString();
//            String s2 = loc2.getURL().normalize().toASCIIString();
//            return Long.compare(s2.length(), s1.length());
//        }).filter(loc -> {
//            String prefix = loc.getURL().normalize().toASCIIString();
//            return requestUri.startsWith(prefix);
//        }).map(mavenRepositorySettings::getCredentials).filter(Objects::nonNull).findFirst()
//                .orElse(null);
//    });
	}

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
		if (DEBUG) {
			logger.info("Request stream for " + toDownload + "...");
		}
		requests.increment();
		if (toDownload.toASCIIString().endsWith("p2.index")) {
			indexRequests.increment();
		}
		try {
			File cachedFile = getCachedFile(toDownload);
			if (cachedFile != null) {
				if (DEBUG) {
					logger.info(" --> routed through http-cache ...");
				}
				return new FileInputStream(cachedFile);
			}
			return toDownload.toURL().openStream();
		} catch (FileNotFoundException e) {
			if (DEBUG) {
				logger.info(" --> not found!");
			}
			throw e;
		} catch (IOException e) {
			if (DEBUG) {
				logger.info(" --> generic error: " + e);
			}
			throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
					"download from " + toDownload + " failed", e));
		} finally {
			if (DEBUG) {
				logger.info("Total number of requests: " + requests.longValue() + " (" + indexRequests.longValue()
						+ " for p2.index)");
			}
		}
	}

	@Override
	public long getLastModified(URI toDownload, IProgressMonitor monitor)
			throws CoreException, FileNotFoundException, AuthenticationFailedException {
		// TODO P2 cache manager relies on this method to throw an exception to work
		// correctly
		try {
			if (isHttp(toDownload)) {
				return httpCache.getCacheEntry(toDownload, logger).getLastModified(proxyService.get(),
						credentialsProvider);
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

	@Override
	public Object createService(IProvisioningAgent agent) {
		return this;
	}

	public SharedHttpCacheStorage getHttpCache() {
		return httpCache;
	}

	public File getCachedFile(URI remoteFile) throws IOException {

		if (isHttp(remoteFile)) {
			return httpCache.getCacheEntry(remoteFile, logger).getCacheFile(proxyService.get(), credentialsProvider);
		}
		return null;
	}

	public static boolean isHttp(URI remoteFile) {
		String scheme = remoteFile.getScheme();
		return scheme != null && scheme.toLowerCase().startsWith("http");
	}

	@Override
	public void afterFrameworkStarted(EmbeddedEquinox framework) {
		// TODO drop ECF transport!
		if (!"ecf".equalsIgnoreCase(System.getProperty("tycho.p2.transport"))) {
			MavenSession session = legacySupport.getSession();
			File cacheLocation = new File(session.getLocalRepository().getBasedir(), ".cache/tycho");
			cacheLocation.mkdirs();
			boolean offline = session.isOffline();
			boolean updateSnapshots = session.getRequest().isUpdateSnapshots();
			proxyService = framework.getServiceFactory().getServiceSupplier(IProxyService.class);
			logger.info(
					"### Using TychoRepositoryTransport for remote P2 access (You can disable this with -Dtycho.p2.transport=ecf) ###");
			logger.info("    Cache location:         " + cacheLocation);
			logger.info("    Transport mode:         " + (offline ? "offline" : "online"));
			logger.info("    Update mode:            " + (updateSnapshots ? "forced" : "cache first"));
			logger.info("    Minimum cache duration: " + SharedHttpCacheStorage.MIN_CACHE_PERIOD + " minutes");
			logger.info(
					"      (you can configure this with -Dtycho.p2.transport.min-cache-minutes=<desired minimum cache duration>)");
			numberFormat.setMaximumFractionDigits(2);
			// TODO can we have this simply as a plexus component?
			httpCache = SharedHttpCacheStorage.getStorage(cacheLocation, offline, updateSnapshots);
			// TODO should become an own plexus component once we permanently moved away
			// from ECF!
			TychoRepositoryTransportCacheManager cacheManager = new TychoRepositoryTransportCacheManager(this,
					new File(session.getLocalRepository().getBasedir()));
			framework.registerService(IAgentServiceFactory.class, this,
					Map.of(Constants.SERVICE_RANKING, 100, IAgentServiceFactory.PROP_CREATED_SERVICE_NAME,
							org.eclipse.equinox.internal.p2.repository.Transport.SERVICE_NAME));
			framework.registerService(IAgentServiceFactory.class, cacheManager, Map.of(Constants.SERVICE_RANKING, 100,
					IAgentServiceFactory.PROP_CREATED_SERVICE_NAME, CacheManager.SERVICE_NAME));
		}
	}

}
