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
package org.eclipse.tycho.agent;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.internal.p2.repository.CacheManagerComponent;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.tycho.core.shared.MavenContext;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.internal.p2.repository.CacheManager")
public class TychoRepositoryTransportCacheManagerAgentFactory implements IAgentServiceFactory {

    @Requirement
    private MavenContext mavenContext;

    @Override
    public Object createService(IProvisioningAgent agent) {
        Object transport = agent.getService(Transport.SERVICE_NAME);
        if (transport instanceof TychoRepositoryTransport tychoRepositoryTransport) {
            return new TychoRepositoryTransportCacheManager(tychoRepositoryTransport, mavenContext);
        }
        return new CacheManagerComponent().createService(agent);
    }

}
