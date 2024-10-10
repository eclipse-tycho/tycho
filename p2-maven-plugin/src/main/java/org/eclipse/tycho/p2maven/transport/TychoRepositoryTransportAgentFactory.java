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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("org.eclipse.equinox.internal.p2.repository.Transport")
public class TychoRepositoryTransportAgentFactory implements IAgentServiceFactory {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final AtomicBoolean infoPrinted = new AtomicBoolean();

	private final TransportCacheConfig transportCacheConfig;
	private final Transport repositoryTransport;

	@Inject
	public TychoRepositoryTransportAgentFactory(TransportCacheConfig transportCacheConfig,
												@Named("tycho") Transport repositoryTransport) {
		this.transportCacheConfig = transportCacheConfig;
		this.repositoryTransport = repositoryTransport;
	}

	@Override
    public Object createService(IProvisioningAgent agent) {
		if (infoPrinted.compareAndSet(false, true)) {
			logger.info("### Using TychoRepositoryTransport for remote P2 access ###");
			logger.info("    Cache location:         " + transportCacheConfig.getCacheLocation());
			logger.info("    Transport mode:         " + (transportCacheConfig.isOffline() ? "offline" : "online"));
			logger.info("    Http Transport type:    " + HttpTransportProtocolHandler.TRANSPORT_TYPE);
			logger.info("    Update mode:            " + (transportCacheConfig.isUpdate() ? "forced" : "cache first"));
			logger.info("    Minimum cache duration: " + SharedHttpCacheStorage.MIN_CACHE_PERIOD + " minutes");
			logger.info(
					"      (you can configure this with -Dtycho.p2.transport.min-cache-minutes=<desired minimum cache duration>)");
		}
		return repositoryTransport;
    }
}
