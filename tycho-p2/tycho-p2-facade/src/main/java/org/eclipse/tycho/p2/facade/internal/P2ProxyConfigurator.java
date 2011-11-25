/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.equinox.embedder.EquinoxEmbedder;
import org.eclipse.tycho.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.p2.metadata.ProxyServiceFacade;

@Component(role = EquinoxLifecycleListener.class, hint = "P2ProxyConfigurator")
public class P2ProxyConfigurator extends EquinoxLifecycleListener {
    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Override
    public void afterFrameworkStarted(EquinoxEmbedder framework) {
        MavenSession session = context.getSession();

        final List<Proxy> activeProxies = new ArrayList<Proxy>();
        for (Proxy proxy : session.getSettings().getProxies()) {
            if (proxy.isActive()) {
                activeProxies.add(proxy);
            }
        }

        ProxyServiceFacade proxyService;
        proxyService = framework.getServiceFactory().getService(ProxyServiceFacade.class);
        // make sure there is no old state from previous aborted builds
        logger.debug("clear OSGi proxy settings");
        proxyService.clearPersistentProxySettings();
        for (Proxy proxy : activeProxies) {
            logger.debug("Configure OSGi proxy for protocol " + proxy.getProtocol() + ", host: " + proxy.getHost()
                    + ", port: " + proxy.getPort() + ", nonProxyHosts: " + proxy.getNonProxyHosts());
            proxyService.configureProxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                    proxy.getPassword(), proxy.getNonProxyHosts());
        }
    }

}
