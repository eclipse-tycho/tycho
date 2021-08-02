/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;

/**
 * p2 {@link CacheManager} instance caching the p2 repository indices (i.e. <tt>content.xml</tt> and
 * <tt>artifacts.xml</tt>) in the local Maven repository.
 */
@SuppressWarnings("restriction")
class RemoteRepositoryCacheManager extends CacheManager {
    public static final String CACHE_RELPATH = ".cache/tycho/p2-repository-metadata";

    private final boolean offline;

    private final File localRepositoryLocation;

    private final MavenLogger logger;

    public RemoteRepositoryCacheManager(Transport transport, MavenContext mavenContext) {
        super(null, transport);

        this.localRepositoryLocation = mavenContext.getLocalRepositoryRoot();
        this.offline = mavenContext.isOffline();
        this.logger = mavenContext.getLogger();
        if (logger == null)
            throw new NullPointerException();
    }

    @Override
    public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor)
            throws IOException, ProvisionException {
        File cacheFile = getCache(repositoryLocation, prefix);
        if (offline) {
            if (cacheFile != null) {
                return cacheFile;
            }
            throw new ProvisionException(getFailureStatus(repositoryLocation));
        } else {
            /**
             * Here, we could implement a cache refreshment policy, but the
             * AbstractRepositoryManager keeps soft references to loaded repositories which turns
             * out to be sufficient.
             */
            try {
                return super.createCache(repositoryLocation, prefix, monitor);
            } catch (IOException e) {
                return handleCreateCacheException(cacheFile, repositoryLocation, e);
            } catch (ProvisionException e) {
                return handleCreateCacheException(cacheFile, repositoryLocation, e);
            }
        }
    }

    @Override
    public File createCacheFromFile(URI remoteFile, IProgressMonitor monitor) throws ProvisionException, IOException {
        File cacheFile = getCacheFile(remoteFile, getCacheDirectory());
        if (offline) {
            if (cacheFile != null) {
                return cacheFile;
            }
            throw new ProvisionException(getFailureStatus(remoteFile));
        }
        try {
            return super.createCacheFromFile(remoteFile, monitor);
        } catch (IOException e) {
            return handleCreateCacheException(cacheFile, remoteFile, e);
        } catch (ProvisionException e) {
            return handleCreateCacheException(cacheFile, remoteFile, e);
        }
    }

    private Status getFailureStatus(URI uri) throws ProvisionException {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, ProvisionException.REPOSITORY_NOT_FOUND,
                "Repository system is offline and no local cache available for " + uri.toString(), null);
    }

    // this method is copied from the super class, because its private there
    // we calculate the hashcode the same way as p2 and we must 
    // keep the hashcode calculation in sync with them
    public static File getCacheFile(URI url, File dataAreaFile) {
        int hashCode = url.hashCode();
        return new File(dataAreaFile, Integer.toString(hashCode));
    }

    private File handleCreateCacheException(File cacheFile, URI repositoryLocation, Exception e)
            throws ProvisionException {
        if (cacheFile != null) {
            String message = "Failed to access p2 repository " + repositoryLocation.toASCIIString()
                    + ", use local cache.";
            if (logger.isDebugEnabled()) {
                logger.warn(message, e);
            } else {
                message += " " + e.getMessage();
                logger.warn(message);
            }
            // original exception has been already logged
            return cacheFile;
        }
        throw new ProvisionException(getFailureStatus(repositoryLocation));
    }

    @Override
    protected File getCacheDirectory() {
        return new File(localRepositoryLocation, CACHE_RELPATH);
    }

}
