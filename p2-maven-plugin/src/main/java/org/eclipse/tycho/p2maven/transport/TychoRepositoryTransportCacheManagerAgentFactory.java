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
package org.eclipse.tycho.p2maven.transport;

import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.equinox.internal.p2.repository.CacheManagerComponent;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

@Named("org.eclipse.equinox.internal.p2.repository.CacheManager")
@Singleton
public class TychoRepositoryTransportCacheManagerAgentFactory implements IAgentServiceFactory {

    @Override
    public Object createService(IProvisioningAgent agent) {
        Object transport = agent.getService(Transport.SERVICE_NAME);
        if (transport instanceof TychoRepositoryTransport tychoRepositoryTransport) {
			return new TychoRepositoryTransportCacheManager(tychoRepositoryTransport);
        }
        return new CacheManagerComponent().createService(agent);
    }

}
