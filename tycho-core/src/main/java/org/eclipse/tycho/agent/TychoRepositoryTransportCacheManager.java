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
package org.eclipse.tycho.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.shared.MavenContext;

@SuppressWarnings("restriction")
public class TychoRepositoryTransportCacheManager extends CacheManager {

    public static final String CACHE_RELPATH = ".cache/tycho/p2-repository-metadata";

    private static final List<String> EXTENSIONS = List.of(".jar", ".xml");

    private MavenContext mavenContext;
    private TychoRepositoryTransport transport;

    public TychoRepositoryTransportCacheManager(TychoRepositoryTransport transport, MavenContext mavenContext) {
        super(null, transport);
        this.transport = transport;
        this.mavenContext = mavenContext;
    }

    @Override
    public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor)
            throws IOException, ProvisionException {
        if (TychoRepositoryTransport.isHttp(repositoryLocation)) {
            for (String extension : EXTENSIONS) {
                URI fileLocation = URIUtil.append(repositoryLocation, prefix + extension);
                try {
                    File cachedFile = transport.getCachedFile(fileLocation);
                    if (cachedFile != null) {
                        return cachedFile;
                    }
                } catch (FileNotFoundException e) {
                    continue;
                }
            }
            throw new FileNotFoundException(
                    "Not found any of " + EXTENSIONS + " for " + repositoryLocation + " with prefix " + prefix);
        }
        return super.createCache(repositoryLocation, prefix, monitor);
    }

    @Override
    public File createCacheFromFile(URI remoteFile, IProgressMonitor monitor) throws ProvisionException, IOException {
        File cachedFile = transport.getCachedFile(remoteFile);
        if (cachedFile != null) {
            //no need to cache this twice ...
            return cachedFile;
        }
        return super.createCacheFromFile(remoteFile, monitor);
    }

    @Override
    protected File getCacheDirectory() {
        return new File(mavenContext.getLocalRepositoryRoot(), CACHE_RELPATH);
    }

}
