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

import java.net.URI;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;

@Component(role = IProvisioningAgentProvider.class)
public class DefaultProvisioningAgentProvider implements IProvisioningAgentProvider {

	@Requirement(hint = "connect")
	private EquinoxServiceFactory serviceFactory;

	@Override
	public IProvisioningAgent createAgent(URI location) throws ProvisionException {
		IProvisioningAgentProvider provider = serviceFactory.getService(IProvisioningAgentProvider.class);
		if (provider == null) {
			throw new IllegalStateException("can't acquire IProvisioningAgentProvider service!");
		}
		return provider.createAgent(location);
	}

}
