/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote.test.util;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.tycho.p2.remote.IRepositoryIdManager;
import org.eclipse.tycho.test.util.P2Context;

/**
 * This factory is registered as {@link IAgentServiceFactory} service so that dummy
 * {@link IRepositoryIdManager} instances are automatically created (as fall-back) in the p2 agents
 * used by tests. This factory in not used by the productive Tycho code.
 * 
 * @see P2Context
 */
public class NoopRepositoryIdManagerFactory implements IAgentServiceFactory {

    public Object createService(IProvisioningAgent agent) {
        return new NoopRepositoryIdManager();
    }

}
