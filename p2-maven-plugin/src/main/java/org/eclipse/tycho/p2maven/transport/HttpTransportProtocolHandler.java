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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.transport.TransportProtocolHandler;

@Named("http")
@Singleton
public class HttpTransportProtocolHandler implements TransportProtocolHandler {

	static final String TRANSPORT_TYPE = System.getProperty("tycho.p2.httptransport.type",
			Java11HttpTransportFactory.HINT);

	@Inject
	Map<String, HttpTransportFactory> transportFactoryMap;
	@Inject
	HttpCache httpCache;

	@Inject
	Logger logger;

	private HttpTransportFactory getTransportFactory() {
		return Objects.requireNonNull(transportFactoryMap.get(TRANSPORT_TYPE), "Invalid transport configuration");
	}

	@Override
	public long getLastModified(URI uri) throws IOException {
		return httpCache.getCacheEntry(uri, logger).getLastModified(getTransportFactory());
	}

	@Override
	public File getFile(URI remoteFile) throws IOException {
		return httpCache.getCacheEntry(remoteFile, logger).getCacheFile(getTransportFactory());
	}

}
