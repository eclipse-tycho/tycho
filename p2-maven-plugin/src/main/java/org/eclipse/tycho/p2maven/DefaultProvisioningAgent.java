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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class DefaultProvisioningAgent implements IProvisioningAgent {

	static {
		MirrorSelector.MIRROR_PARSE_ERROR_LEVEL = IStatus.INFO;
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, Supplier<Object>> agentServices = new ConcurrentHashMap<>();

	private final EquinoxServiceFactory serviceFactory;
	private final Map<String, IAgentServiceFactory> agentFactories;
	private final MavenPropertyHelper propertyHelper;
	private final PlexusContainer plexusContainer; // TODO: get rid of this (or Maven should offer alternative)

	@Inject
	public DefaultProvisioningAgent(@Named("connect") EquinoxServiceFactory serviceFactory,
									Map<String, IAgentServiceFactory> agentFactories,
									MavenPropertyHelper propertyHelper,
									PlexusContainer plexusContainer) {
		this.serviceFactory = serviceFactory;
		this.agentFactories = agentFactories;
		this.propertyHelper = propertyHelper;
		this.plexusContainer = plexusContainer;
	}

	@Override
	public Object getService(String serviceName) {
		Object agentService = getOSGiAgentService(serviceName);
		if (agentService != null) {
			return agentService;
		}
		logger.debug("Service " + serviceName
				+ " not found in OSGi ProvisioningAgent agent, look it up in Plexus AgentServiceFactories");
		Object factoryService = getAgentFactoryService(serviceName);
		if (factoryService != null) {
			return factoryService;
		}
		logger.debug("Service " + serviceName
				+ " not found in Plexus AgentServiceFactories, look it up in Plexus Container");
		try {
			return plexusContainer.lookup(serviceName);
		} catch (ComponentLookupException e) {
			logger.debug("Service " + serviceName + " was not found in PlexusContainer");
		}
		logger.warn("Cannot locate service " + serviceName + " because no provisioning agent was found");
		return null;

	}

	private synchronized Object getAgentFactoryService(String serviceName) {
		return agentServices.computeIfAbsent(serviceName, key -> {
			IAgentServiceFactory factory = agentFactories.get(key);
			if (factory != null) {
				// we must need an indirection here because otherwise there is a chance for
				// recursive updates when the factory creates other services
				return new LazyAgentServiceFactory(factory, DefaultProvisioningAgent.this);
			}
			return () -> null;
		}).get();
	}

	private Object getOSGiAgentService(String serviceName) {
		IProvisioningAgent agent = serviceFactory.getService(IProvisioningAgent.class);
		if (agent != null) {
			return agent.getService(serviceName);
		}
		return null;
	}

	@Override
	public void registerService(String serviceName, Object service) {
		IProvisioningAgent agent = serviceFactory.getService(IProvisioningAgent.class);
		if (agent != null) {
			agent.registerService(serviceName, service);
		}
	}

	@Override
	public void stop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void unregisterService(String serviceName, Object service) {
		IProvisioningAgent agent = serviceFactory.getService(IProvisioningAgent.class);
		if (agent != null) {
			agent.unregisterService(serviceName, service);
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return propertyHelper.getGlobalProperty(key, defaultValue);
	}

	@Override
	public String getProperty(String key) {
		return propertyHelper.getGlobalProperty(key);
	}

	private static final class LazyAgentServiceFactory implements Supplier<Object> {

		private IAgentServiceFactory factory;
		private IProvisioningAgent agent;
		private Object service;

		LazyAgentServiceFactory(IAgentServiceFactory factory, IProvisioningAgent agent) {
			this.factory = factory;
			this.agent = agent;
		}

		@Override
		public synchronized Object get() {
			if (service == null && factory != null && agent != null) {
				// first copy a reference
				IAgentServiceFactory agentServiceFactory = factory;
				IProvisioningAgent provisioningAgent = agent;
				// now clear the global references, just in case this method is getting called
				// again
				factory = null;
				agent = null;
				service = agentServiceFactory.createService(provisioningAgent);
			}
			return service;
		}

	}

	static {
		/*
		 * Workaround for p2 bug 356561: Due to historical reasons, p2 considers a
		 * composite repository to be loaded successfully even though some of its
		 * children failed to load. This is bad for Tycho because it allows for
		 * network/server outages to threaten build reproducibility. Therefore, we
		 * change the composite loading behaviour to be atomic for composite
		 * repositories (except those that explicitly state
		 * p2.atomic.composite.loading=false in their repository properties). This can
		 * be done via a system property (see CompositeArtifactRepository and
		 * CompositeMetadataRepository).
		 */
		String atomicDefaultSystemProperty = "eclipse.p2.atomic.composite.loading.default";

		if (System.getProperty(atomicDefaultSystemProperty) == null) {
			// not explicitly set on command line -> set Tycho's default
			System.setProperty(atomicDefaultSystemProperty, Boolean.toString(true));
		}
	}

}
