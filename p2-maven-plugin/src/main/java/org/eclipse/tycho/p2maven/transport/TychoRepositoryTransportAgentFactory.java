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
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.internal.p2.repository.Transport")
public class TychoRepositoryTransportAgentFactory implements IAgentServiceFactory, Initializable {

	private static final String TRANSPORT_TYPE = System.getProperty("tycho.p2.transport.type",
			Java11HttpTransportFactory.HINT);

	@Requirement
	private LegacySupport legacySupport;

	@Requirement
	private Logger logger;


	@Requirement
	private Map<String, HttpTransportFactory> transportFactoryMap;

	private TychoRepositoryTransport transport;

	@Override
	public Object createService(IProvisioningAgent agent) {
		return transport;
	}

	@Override
	public void initialize() throws InitializationException {
		File repoDir;
		boolean offline;
		boolean update;
		MavenSession session = legacySupport.getSession();
		if (session == null) {
			repoDir = RepositorySystem.defaultUserLocalRepository;
			offline = false;
			update = false;
		} else {
			offline = session.isOffline();
			repoDir = new File(session.getLocalRepository().getBasedir());
			update = session.getRequest().isUpdateSnapshots();
		}
		File cacheLocation = new File(repoDir, ".cache/tycho");
		cacheLocation.mkdirs();
		logger.info("### Using TychoRepositoryTransport for remote P2 access ###");
		logger.info("    Cache location:         " + cacheLocation);
		logger.info("    Transport mode:         " + (offline ? "offline" : "online"));
		logger.info("    Transport type:         " + TRANSPORT_TYPE);
		logger.info("    Update mode:            " + (update ? "forced" : "cache first"));
		logger.info("    Minimum cache duration: " + SharedHttpCacheStorage.MIN_CACHE_PERIOD + " minutes");
		logger.info(
				"      (you can configure this with -Dtycho.p2.transport.min-cache-minutes=<desired minimum cache duration>)");

		SharedHttpCacheStorage cache = SharedHttpCacheStorage.getStorage(cacheLocation, offline, update);
		transport = new TychoRepositoryTransport(logger, cache, transportFactoryMap.get(TRANSPORT_TYPE));
	}

}
