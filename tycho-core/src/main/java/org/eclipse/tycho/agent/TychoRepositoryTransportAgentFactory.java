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

import java.util.Objects;
import java.util.stream.Stream;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.osgi.TychoServiceFactory;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.internal.p2.repository.Transport")
public class TychoRepositoryTransportAgentFactory implements IAgentServiceFactory {

    @Requirement(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory serviceFactory;

    @Requirement
    private MavenContext mavenContext;

    @Requirement
    private MavenRepositorySettings mavenRepositorySettings;

    @Override
    public Object createService(IProvisioningAgent agent) {
        return new TychoRepositoryTransport(mavenContext, serviceFactory.getService(IProxyService.class), uri -> {
            IRepositoryIdManager repositoryIdManager = agent.getService(IRepositoryIdManager.class);
            Stream<MavenRepositoryLocation> locations = mavenContext.getMavenRepositoryLocations();
            locations = Stream.concat(locations, repositoryIdManager.getKnownMavenRepositoryLocations());
            String requestUri = uri.normalize().toASCIIString();
            return locations.sorted((loc1, loc2) -> {
                //we wan't the longest prefix match, so first sort all uris by their length ...
                String s1 = loc1.getURL().normalize().toASCIIString();
                String s2 = loc2.getURL().normalize().toASCIIString();
                return Long.compare(s2.length(), s1.length());
            }).filter(loc -> {
                String prefix = loc.getURL().normalize().toASCIIString();
                return requestUri.startsWith(prefix);
            }).map(mavenRepositorySettings::getCredentials).filter(Objects::nonNull).findFirst().orElse(null);
        });
    }

}
