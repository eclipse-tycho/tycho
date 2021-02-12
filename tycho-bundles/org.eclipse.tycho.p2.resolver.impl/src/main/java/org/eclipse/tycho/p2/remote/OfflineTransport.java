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
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;

/**
 * workarounds for Bug 357357, Bug 571195
 */
@SuppressWarnings("restriction")
class OfflineTransport extends Transport {

    private File cacheLocation;
    private MavenLogger logger;

    public OfflineTransport(MavenContext mavenContext) {
        cacheLocation = new File(mavenContext.getLocalRepositoryRoot(), RemoteRepositoryCacheManager.CACHE_RELPATH);
        logger = mavenContext.getLogger();
    }

    @Override
    public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
        return download(toDownload, target, 0, monitor);
    }

    @Override
    public IStatus download(URI source, OutputStream target, long startPos, IProgressMonitor monitor) {
        try {
            try (InputStream in = stream(source, monitor)) {
                for (long s = 0; s < startPos; s++) {
                    in.read();
                }
                in.transferTo(target);
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    String.format("loading %s from cache at position %d failed", source, startPos), e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public InputStream stream(URI source, IProgressMonitor monitor) throws FileNotFoundException {
        File cacheFile = RemoteRepositoryCacheManager.getCacheFile(source, cacheLocation);
        if (cacheFile != null && cacheFile.exists()) {
            logger.debug(String.format("return cached file %s for uri %s, maven is currently in offline mode",
                    cacheFile, source));
            return new FileInputStream(cacheFile);
        }
        throw new FileNotFoundException(String.format(
                "maven is currently in offline mode but cachefile %s for requested URI %s does not exits locally!",
                cacheFile, source));
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

}
