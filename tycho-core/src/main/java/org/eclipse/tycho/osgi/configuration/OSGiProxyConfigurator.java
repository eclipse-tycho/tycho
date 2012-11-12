/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.util.Locale;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.facade.ProxyServiceFacade;

@Component(role = EquinoxLifecycleListener.class, hint = "P2ProxyConfigurator")
public class OSGiProxyConfigurator extends EquinoxLifecycleListener {
    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Requirement
    private SettingsDecrypter decrypter;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        MavenSession session = context.getSession();

        ProxyServiceFacade proxyService = framework.getServiceFactory().getService(ProxyServiceFacade.class);

        // make sure there is no old state from previous aborted builds
        clearProxyConfiguration(proxyService);

        for (Proxy proxy : session.getSettings().getProxies()) {
            if (proxy.isActive()) {
                setProxy(proxyService, proxy);
            }
        }
    }

    private void clearProxyConfiguration(ProxyServiceFacade proxyService) {
        logger.debug("Clearing proxy settings in OSGi runtime");
        proxyService.clearPersistentProxySettings();
    }

    private void setProxy(ProxyServiceFacade proxyService, Proxy proxy) {
        String protocol = proxy.getProtocol();

        if (isSupportedProtocol(protocol)) {
            logger.debug("Configuring proxy for protocol " + protocol + ": host=" + proxy.getHost() + ", port="
                    + proxy.getPort() + ", nonProxyHosts=" + proxy.getNonProxyHosts() + "");

            DefaultSettingsDecryptionRequest decryptRequest = new DefaultSettingsDecryptionRequest(proxy);
            SettingsDecryptionResult result = decrypter.decrypt(decryptRequest);
            proxy = result.getProxy() != null ? result.getProxy() : proxy;
            if (result.getProxy() == null) {
                for (SettingsProblem problem : result.getProblems()) {
                    logger.info(problem.toString());
                }
            }

            proxyService.configureProxy(protocol, proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                    proxy.getPassword(), proxy.getNonProxyHosts());

        } else {
            logger.debug("Ignoring proxy configuration for unsupported protocol: '" + protocol + "'");
        }
    }

    private boolean isSupportedProtocol(String protocol) {
        if (protocol == null) {
            return false;
        }
        protocol = protocol.trim().toLowerCase(Locale.ENGLISH);
        if ("http".equals(protocol) || "https".equals(protocol) || "socks4".equals(protocol)
                || "socks_5".equals(protocol)) {
            return true;
        }
        return false;
    }

}
