/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote.testutil;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.test.util.P2Context;

/**
 * This factory is registered as {@link IAgentServiceFactory} service so that dummy
 * {@link IRepositoryIdManager} instances are automatically created (as fall-back) in the p2 agents
 * used by tests. This factory in not used by the productive Tycho code.
 * 
 * @see P2Context
 */
public class NoopRepositoryIdManagerFactory implements IAgentServiceFactory {

    @Override
    public Object createService(IProvisioningAgent agent) {
        return new NoopRepositoryIdManager(agent);
    }

}
