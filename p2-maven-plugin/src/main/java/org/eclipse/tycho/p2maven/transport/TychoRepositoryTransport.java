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
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

public class TychoRepositoryTransport extends org.eclipse.equinox.internal.p2.repository.Transport
        implements IAgentServiceFactory {

	private static final boolean DEBUG_REQUESTS = Boolean.getBoolean("tycho.p2.transport.debug");

    private NumberFormat numberFormat = NumberFormat.getNumberInstance();

	private Logger logger;
    private SharedHttpCacheStorage httpCache;
    private LongAdder requests = new LongAdder();
    private LongAdder indexRequests = new LongAdder();

	private HttpTransportFactory transportFactory;

	public TychoRepositoryTransport(Logger logger, SharedHttpCacheStorage httpCache,
			HttpTransportFactory transportFactory) {
		this.logger = logger;
		this.transportFactory = transportFactory;
		this.httpCache = httpCache;
		numberFormat.setMaximumFractionDigits(2);
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
        if (target instanceof IStateful stateful) {
            stateful.setStatus(status);
        }
        return status;
    }

    @Override
    public synchronized InputStream stream(URI toDownload, IProgressMonitor monitor)
            throws FileNotFoundException, CoreException, AuthenticationFailedException {
		if (DEBUG_REQUESTS) {
            logger.debug("Request stream for " + toDownload + "...");
		}
        requests.increment();
        if (toDownload.toASCIIString().endsWith("p2.index")) {
            indexRequests.increment();
        }
        try {
            File cachedFile = getCachedFile(toDownload);
            if (cachedFile != null) {
				if (DEBUG_REQUESTS) {
                    logger.debug(" --> routed through http-cache ...");
                }
                return new FileInputStream(cachedFile);
            }
            return toDownload.toURL().openStream();
        } catch (FileNotFoundException e) {
			if (DEBUG_REQUESTS) {
                logger.debug(" --> not found!");
            }
            throw e;
        } catch (IOException e) {
			if (DEBUG_REQUESTS) {
                logger.debug(" --> generic error: " + e);
            }
            throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
                    "download from " + toDownload + " failed", e));
        } finally {
			if (DEBUG_REQUESTS) {
                logger.debug("Total number of requests: " + requests.longValue() + " (" + indexRequests.longValue()
                        + " for p2.index)");
            }
        }
    }

    @Override
    public long getLastModified(URI toDownload, IProgressMonitor monitor)
            throws CoreException, FileNotFoundException, AuthenticationFailedException {
        //TODO P2 cache manager relies on this method to throw an exception to work correctly
        try {
            if (isHttp(toDownload)) {
				return httpCache.getCacheEntry(toDownload, logger).getLastModified(transportFactory);
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
			return httpCache.getCacheEntry(remoteFile, logger).getCacheFile(transportFactory);
        }
        return null;
    }

    public static boolean isHttp(URI remoteFile) {
        String scheme = remoteFile.getScheme();
        return scheme != null && scheme.toLowerCase().startsWith("http");
    }

}
