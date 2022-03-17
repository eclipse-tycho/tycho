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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.osgi.framework.BundleContext;

@Component(role = IProvisioningAgent.class)
public class DefaultProvisioningAgent implements IProvisioningAgent {

	@Requirement
	private Logger log;

	@Requirement(hint = "plexus")
	private BundleContext bundleContext;

	@Requirement(role = IAgentServiceFactory.class)
	private Map<String, IAgentServiceFactory> factoryMap;

	private Map<String, Object> services = new ConcurrentHashMap<String, Object>();

	@Override
	public Object getService(String serviceName) {
		return services.computeIfAbsent(serviceName, role -> {
			IAgentServiceFactory serviceFactory = factoryMap.get(role);
			if (serviceFactory != null) {
				return serviceFactory.createService(DefaultProvisioningAgent.this);
			}
			return null;
		});
	}

	@Override
	public void registerService(String serviceName, Object service) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void stop() {

	}

	@Override
	public void unregisterService(String serviceName, Object service) {
		throw new UnsupportedOperationException();
	}

}
