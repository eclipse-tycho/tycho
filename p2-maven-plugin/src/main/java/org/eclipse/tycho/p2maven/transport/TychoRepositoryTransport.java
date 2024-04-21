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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

@Component(role = org.eclipse.equinox.internal.p2.repository.Transport.class, hint = "tycho")
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

	private NumberFormat numberFormat = NumberFormat.getNumberInstance();

	@Requirement
	Logger logger;

	@Requirement
	TransportCacheConfig cacheConfig;

	@Requirement(role = TransportProtocolHandler.class)
	Map<String, TransportProtocolHandler> transportProtocolHandlers;

	private LongAdder requests = new LongAdder();
	private LongAdder indexRequests = new LongAdder();

	public TychoRepositoryTransport() {
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
		return downloadArtifact(toDownload, target, null, monitor);
	}

	private IStatus reportStatus(IStatus status, OutputStream target) {
		if (target instanceof IStateful stateful) {
			stateful.setStatus(status);
		}
		return status;
	}

	@Override
	public IStatus downloadArtifact(URI source, OutputStream target, IArtifactDescriptor descriptor,
			IProgressMonitor monitor) {
		if (descriptor != null) {
			// TODO query the p2 pools!
			Path cacheLocation = getCacheLocation(descriptor);
			if (Files.isRegularFile(cacheLocation)) {
				// TODO need to check local properties!
				try {
					Files.copy(cacheLocation, target);
				} catch (IOException e) {
					return reportStatus(Status.error("download from " + source + " failed", e), target);
				}
				return reportStatus(Status.OK_STATUS, target);
			} else {
				// TODO need to download to cache!
			}
		}
		String id = "p2"; // TODO we might compute the id from the IRepositoryIdManager based on the URI?
		boolean printMessage = cacheConfig.isInteractive() && !cacheConfig.isOffline();
		if (printMessage) {
			logger.info("Downloading from " + id + ": " + source);
		}
		try {
			DownloadStatusOutputStream statusOutputStream = new DownloadStatusOutputStream(target,
					"Download of " + source);
			stream(source, monitor).transferTo(statusOutputStream);
			DownloadStatus downloadStatus = statusOutputStream.getStatus();
			if (printMessage) {
				logger.info("Downloaded from " + id + ": " + source + " ("
						+ FileUtils.byteCountToDisplaySize(downloadStatus.getFileSize()) + " at "
						+ FileUtils.byteCountToDisplaySize(downloadStatus.getTransferRate()) + "/s)");
			}
			return reportStatus(downloadStatus, target);
		} catch (AuthenticationFailedException e) {
			return Status.error("authentication failed for " + source, e);
		} catch (IOException e) {
			return reportStatus(Status.error("download from " + source + " failed", e), target);
		} catch (CoreException e) {
			return reportStatus(e.getStatus(), target);
		}
	}

	private Path getCacheLocation(IArtifactDescriptor descriptor) {
		Path location = cacheConfig.getCacheLocation().toPath();
		IArtifactKey artifactKey = descriptor.getArtifactKey();
		// TODO check the maven properties and construct a path to the local repo!
		return location.resolve(getClassifier(artifactKey)).resolve(artifactKey.getId())
				.resolve(getVersion(artifactKey));
	}

	private String getVersion(IArtifactKey artifactKey) {
		Version version = artifactKey.getVersion();
		if (version == null) {
			return "0.0.0";
		}
		return version.toString();
	}

	private String getClassifier(IArtifactKey artifactKey) {
		String classifier = artifactKey.getClassifier();
		if (classifier == null || classifier.isBlank()) {
			return "artifacts";
		}
		return classifier;
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
