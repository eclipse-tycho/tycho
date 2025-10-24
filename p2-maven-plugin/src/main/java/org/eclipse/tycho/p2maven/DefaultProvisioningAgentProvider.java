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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;

@Named
@Singleton
public class DefaultProvisioningAgentProvider implements IProvisioningAgentProvider {

	@Inject
	@Named("connect")
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
