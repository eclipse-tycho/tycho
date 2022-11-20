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
package org.eclipse.tycho.p2maven.repository;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.tycho.IRepositoryIdManager;

@Component(role = IAgentServiceFactory.class, hint = IRepositoryIdManager.SERVICE_NAME)
public class RepositoryIdManagerAgentServiceFactory implements IAgentServiceFactory {

	@Requirement
	IRepositoryIdManager repositoryIdManager;

	@Override
	public Object createService(IProvisioningAgent agent) {
		return repositoryIdManager;
	}

}
