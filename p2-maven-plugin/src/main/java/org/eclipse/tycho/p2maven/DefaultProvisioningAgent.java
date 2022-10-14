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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;

@Component(role = IProvisioningAgent.class)
public class DefaultProvisioningAgent implements IProvisioningAgent {

	@Requirement
	private Logger log;

	@Requirement(hint = "connect")
	private EquinoxServiceFactory serviceFactory;

	@Override
	public Object getService(String serviceName) {
		IProvisioningAgent agent = serviceFactory.getService(IProvisioningAgent.class);
		if (agent != null) {
			return agent.getService(serviceName);
		}
		log.warn("Can't locate service = " + serviceName + " because no provisioning agent was found!");
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

}
