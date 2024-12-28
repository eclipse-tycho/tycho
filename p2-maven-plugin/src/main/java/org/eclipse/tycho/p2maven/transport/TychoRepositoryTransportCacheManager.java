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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.transport.TransportProtocolHandler;

public class TychoRepositoryTransportCacheManager extends CacheManager {

    private static final List<String> EXTENSIONS = List.of(".jar", ".xml");

    private TychoRepositoryTransport transport;

	public TychoRepositoryTransportCacheManager(TychoRepositoryTransport transport) {
        super(null, transport);
        this.transport = transport;
    }

    @Override
    public File createCache(URI repositoryLocation, String prefix, IProgressMonitor monitor)
            throws IOException, ProvisionException {
		TransportProtocolHandler handler = transport.getHandler(repositoryLocation);
		if (handler != null) {
			for (String extension : EXTENSIONS) {
				URI fileLocation = URIUtil.append(repositoryLocation, prefix + extension);
				try {
					File cachedFile = handler.getFile(fileLocation);
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
		TransportProtocolHandler handler = transport.getHandler(remoteFile);
		if (handler != null) {
			File cachedFile = handler.getFile(remoteFile);
			if (cachedFile != null) {
				// no need to cache this twice ...
				return cachedFile;
			}
		}
        return super.createCacheFromFile(remoteFile, monitor);
    }

    @Override
    protected File getCacheDirectory() {

		TransportCacheConfig config = transport.getCacheConfig();
		return new File(config.getCacheLocation(), "p2-repository-metadata");
    }

}
