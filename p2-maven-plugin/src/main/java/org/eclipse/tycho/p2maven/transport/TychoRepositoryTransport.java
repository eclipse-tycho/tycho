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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("tycho")
public class TychoRepositoryTransport extends org.eclipse.equinox.internal.p2.repository.Transport
		implements IAgentServiceFactory {

	private static final int MAX_DOWNLOAD_THREADS = Integer.getInteger("tycho.p2.transport.max-download-threads", 4);

	private static final boolean DEBUG_REQUESTS = Boolean.getBoolean("tycho.p2.transport.debug");

	private static final Executor DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(MAX_DOWNLOAD_THREADS,
			new ThreadFactory() {

				private AtomicInteger cnt = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread thread = new Thread(r);
					thread.setName("Tycho-Download-Thread-" + cnt.getAndIncrement());
					thread.setDaemon(true);
					return thread;
				}
			});

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final NumberFormat numberFormat = NumberFormat.getNumberInstance();
	private final LongAdder requests = new LongAdder();
	private final LongAdder indexRequests = new LongAdder();

	private final TransportCacheConfig cacheConfig;
	private final Map<String, TransportProtocolHandler> transportProtocolHandlers;

	@Inject
	public TychoRepositoryTransport(TransportCacheConfig cacheConfig, Map<String, TransportProtocolHandler> transportProtocolHandlers) {
		this.cacheConfig = cacheConfig;
		this.transportProtocolHandlers = transportProtocolHandlers;

		numberFormat.setMaximumFractionDigits(2);
	}

	@Override
	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
		if (startPos > 0) {
			return Status.error("range downloads are not implemented");
		}
		return download(toDownload, target, monitor);
	}

	@Override
	public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
		String id = "p2"; // TODO we might compute the id from the IRepositoryIdManager based on the URI?
		if (cacheConfig.isInteractive()) {
			logger.info("Downloading from " + id + ": " + toDownload);
		}
		try {
			DownloadStatusOutputStream statusOutputStream = new DownloadStatusOutputStream(target,
					"Download of " + toDownload);
			stream(toDownload, monitor).transferTo(statusOutputStream);
			DownloadStatus downloadStatus = statusOutputStream.getStatus();
			if (cacheConfig.isInteractive()) {
				logger.info("Downloaded from " + id + ": " + toDownload + " ("
						+ FileUtils.byteCountToDisplaySize(downloadStatus.getFileSize()) + " at "
						+ FileUtils.byteCountToDisplaySize(downloadStatus.getTransferRate()) + "/s)");
			}
			return reportStatus(downloadStatus, target);
		} catch (AuthenticationFailedException e) {
			return Status.error("authentication failed for " + toDownload, e);
		} catch (IOException e) {
			return reportStatus(Status.error("download from " + toDownload + " failed", e), target);
		} catch (CoreException e) {
			return reportStatus(e.getStatus(), target);
		}
	}

	private IStatus reportStatus(IStatus status, OutputStream target) {
		if (target instanceof IStateful stateful) {
			stateful.setStatus(status);
		}
		return status;
	}

	@Override
	public InputStream stream(URI toDownload, IProgressMonitor monitor)
			throws FileNotFoundException, CoreException, AuthenticationFailedException {
		if (DEBUG_REQUESTS) {
			logger.debug("Request stream for " + toDownload);
		}
		requests.increment();
		if (toDownload.toASCIIString().endsWith("p2.index")) {
			indexRequests.increment();
		}
		try {
			TransportProtocolHandler handler = getHandler(toDownload);
			if (handler != null) {
				File cachedFile = handler.getFile(toDownload);
				if (cachedFile != null) {
					if (DEBUG_REQUESTS) {
						logger.debug(" --> routed through handler " + handler.getClass().getSimpleName());
					}
					return new FileInputStream(cachedFile);
				}
			}
			return toDownload.toURL().openStream();
		} catch (FileNotFoundException e) {
			if (DEBUG_REQUESTS) {
				logger.debug(" --> not found!");
			}
			throw e;
		} catch (IOException e) {
			if (e instanceof AuthenticationFailedException afe) {
				throw afe;
			}
			if (DEBUG_REQUESTS) {
				logger.debug(" --> generic error: " + e);
			}
			throw new CoreException(Status.error("download from " + toDownload + " failed", e));
		} finally {
			if (DEBUG_REQUESTS) {
				logger.debug("Total number of requests: " + requests.longValue() + " (" + indexRequests.longValue()
						+ " for p2.index)");
			}
		}
	}

	TransportProtocolHandler getHandler(URI uri) {
		String scheme = uri.getScheme();
		if (scheme != null) {
			String lc = scheme.toLowerCase();
			TransportProtocolHandler handler = transportProtocolHandlers.get(lc);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public long getLastModified(URI toDownload, IProgressMonitor monitor)
			throws CoreException, FileNotFoundException, AuthenticationFailedException {
		try {
			TransportProtocolHandler handler = getHandler(toDownload);
			if (handler != null) {
				return handler.getLastModified(toDownload);
			}
			URLConnection connection = toDownload.toURL().openConnection();
			long lastModified = connection.getLastModified();
			connection.getInputStream().close();
			return lastModified;
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw new CoreException(Status.error("download from " + toDownload + " failed", e));
		}
	}

	@Override
	public Object createService(IProvisioningAgent agent) {
		return this;
	}

	public static Executor getDownloadExecutor() {
		return DOWNLOAD_EXECUTOR;
	}

	public File downloadToFile(URI uri) throws IOException {
		TransportProtocolHandler handler = getHandler(uri);
		if (handler != null) {
			File file = handler.getFile(uri);
			if (file != null) {
				return file;
			}
		}
		Path tempFile = Files.createTempFile("tycho", ".tmp");
		tempFile.toFile().deleteOnExit();
		try {
			Files.copy(stream(uri, null), tempFile);
			return tempFile.toFile();
		} catch (CoreException e) {
			throw new IOException(e);
		}
	}

}
