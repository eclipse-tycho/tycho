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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;

@Component(role = IProvisioningAgent.class)
public class DefaultProvisioningAgent implements IProvisioningAgent {

	@Requirement
	private Logger log;

	@Requirement(hint = "connect")
	private EquinoxServiceFactory serviceFactory;

	@Requirement
	private PlexusContainer plexusContainer;

	@Requirement
	Map<String, IAgentServiceFactory> agentFactories;

	private Map<String, Optional<Object>> agentServices = new ConcurrentHashMap<>();

	@Override
	public Object getService(String serviceName) {
		Object agentService = getOSGiAgentService(serviceName);
		if (agentService != null) {
			return agentService;
		}
		log.debug("Service " + serviceName
				+ " not found in OSGi ProvisioningAgent agent, look it up in Plexus AgentServiceFactories");
		Object factoryService = getAgentFactoryService(serviceName);
		if (factoryService != null) {
			return factoryService;
		}
		log.debug("Service " + serviceName
				+ " not found in Plexus AgentServiceFactories, look it up in Plexus Container");
		try {
			return plexusContainer.lookup(serviceName);
		} catch (ComponentLookupException e) {
			log.debug("Service " + serviceName + " was not found in PlexusContainer");
		}
		log.warn("Can't locate service = " + serviceName + " because no provisioning agent was found!");
		return null;

	}

	private Object getAgentFactoryService(String serviceName) {
		return agentServices.computeIfAbsent(serviceName, key -> {
			IAgentServiceFactory factory = agentFactories.get(key);
			if (factory != null) {
				return Optional.ofNullable(factory.createService(DefaultProvisioningAgent.this));
			}
			return Optional.empty();
		}).orElse(null);
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
