/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

@Named
public class DefaultSimpleArtifactRepositoryFactory extends SimpleArtifactRepositoryFactory {

	private IProvisioningAgent agent;

	@Inject
	public DefaultSimpleArtifactRepositoryFactory(IProvisioningAgent agent) {
		this.agent = agent;
	}

	@Override
	protected IProvisioningAgent getAgent() {
		return agent;
	}

}
