/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Bug 570960 - Generate IU of launcher with machine architecture for Mac
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.impl.Activator;

/**
 * workarounds for Bug 357357, Bug 571195
 */
@SuppressWarnings("restriction")
class OfflineTransport extends Transport {

    private File cacheLocation;

    public OfflineTransport(MavenContext mavenContext) {
        cacheLocation = new File(mavenContext.getLocalRepositoryRoot(), RemoteRepositoryCacheManager.CACHE_RELPATH);
    }

    @Override
    public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
        File cacheFile = RemoteRepositoryCacheManager.getCacheFile(toDownload, cacheLocation);
        if (cacheFile != null && cacheFile.exists()) {
            try {
                try (InputStream in = new FileInputStream(cacheFile)) {
                    for (long s = 0; s < startPos; s++) {
                        in.read(); //not very efficient but safe... actually this method should never be called but we include it for completeness
                    }
                    in.transferTo(target);
                }
            } catch (IOException e) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "loading file from cache " + cacheFile + " failed", e);
            }
        }
        return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, createMessage(toDownload, "download@" + startPos));
    }

    /*
     * This is the only method expected to be called in offline mode.
     * 
     * @see AbstractRepositoryManager#loadIndexFile(URI location, IProgressMonitor monitor)
     */
    @Override
    public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
        File cacheFile = RemoteRepositoryCacheManager.getCacheFile(toDownload, cacheLocation);
        if (cacheFile != null && cacheFile.exists()) {
            try {
                try (InputStream in = new FileInputStream(cacheFile)) {
                    in.transferTo(target);
                }
            } catch (IOException e) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "loading file from cache " + cacheFile + " failed", e);
            }
        }
        return new Status(IStatus.CANCEL, Activator.PLUGIN_ID,
                createMessage(toDownload, "download(" + cacheFile + ")"));
    }

    @Override
    public InputStream stream(URI toDownload, IProgressMonitor monitor)
            throws FileNotFoundException, CoreException, AuthenticationFailedException {
        File cacheFile = RemoteRepositoryCacheManager.getCacheFile(toDownload, cacheLocation);
        if (cacheFile != null && cacheFile.exists()) {
            return new FileInputStream(cacheFile);
        }
        throw new FileNotFoundException(createMessage(toDownload, "stream"));
    }

    @Override
    public long getLastModified(URI toDownload, IProgressMonitor monitor)
            throws CoreException, FileNotFoundException, AuthenticationFailedException {
        File cacheFile = RemoteRepositoryCacheManager.getCacheFile(toDownload, cacheLocation);
        if (cacheFile != null && cacheFile.exists()) {
            return cacheFile.lastModified();
        }
        return -1;
    }

    private static String createMessage(URI uri, String method) {
        return String.format("maven is currently in offline mode, requested URI: %s [%s]", uri, method);
    }
}
