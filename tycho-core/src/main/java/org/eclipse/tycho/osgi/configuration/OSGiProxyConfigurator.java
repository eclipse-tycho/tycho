/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.internal.net.Activator;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.p2maven.helper.SettingsDecrypterHelper;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

//TODO this should aktually be part of the p2 maven plugin!
@Component(role = EquinoxLifecycleListener.class, hint = "P2ProxyConfigurator")
public class OSGiProxyConfigurator implements EquinoxLifecycleListener {

    private static final String MAVEN_SETTINGS_SOURCE = "MAVEN_SETTINGS";

    private static final Pattern NON_PROXY_DELIMITERS = Pattern.compile("\\s*[|,]\\s*");

    @Requirement
    protected Logger logger;
    @Requirement
    protected LegacySupport context;

    @Requirement
    protected SettingsDecrypterHelper decrypter;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        MavenSession session = context.getSession();

        IProxyService proxyService = framework.getServiceFactory().getService(IProxyService.class);
        if (proxyService == null) {
            return;
        }
        // make sure there is no old state from previous aborted builds
        clearPersistentProxySettings();

        for (Proxy proxy : session.getSettings().getProxies()) {
            if (proxy.isActive()) {
                setProxy(proxyService, proxy);
            }
        }
    }

    private void setProxy(IProxyService proxyService, Proxy proxy) {
        String protocol = proxy.getProtocol();

        if (isSupportedProtocol(protocol)) {

            SettingsDecryptionResult result = decrypter.decryptAndLogProblems(proxy);
            proxy = result.getProxy();
            logger.debug("Configuring proxy for protocol " + protocol + ": host=" + proxy.getHost() + ", port="
                    + proxy.getPort() + ", nonProxyHosts=" + proxy.getNonProxyHosts());
            configureProxy(proxyService, protocol, proxy.getHost(), proxy.getPort(), proxy.getUsername(),
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

    private void configureProxy(IProxyService proxyService, String protocol, String host, int port, String user,
            String password, String nonProxyHosts) {
        ProxyData proxyData = new ProxyData(getProxyType(protocol));
        proxyData.setHost(host);
        proxyData.setPort(port);
        proxyData.setUserid(user);
        proxyData.setPassword(password);
        proxyData.setSource(MAVEN_SETTINGS_SOURCE);
        try {
            proxyService.setProxyData(new IProxyData[] { proxyData });
            if (nonProxyHosts != null && !nonProxyHosts.trim().isEmpty()) {
                proxyService.setNonProxiedHosts(NON_PROXY_DELIMITERS.split(nonProxyHosts.trim()));
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        // have to register authenticator manually as this is provided as extension point in
        // org.eclipse.ui.net only ...
        registerAuthenticator(user, password);
        proxyService.setProxiesEnabled(true);
        // disable the eclipse native proxy providers
        proxyService.setSystemProxiesEnabled(false);
    }

    private void registerAuthenticator(final String user, final String password) {
        if (user == null || password == null) {
            return;
        }
        Authenticator authenticator = new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }

        };
        // not exactly pretty but this is how org.eclipse.core.net does it
        Authenticator.setDefault(authenticator);
    }

    private static String getProxyType(String protocol) {
        protocol = protocol.trim().toLowerCase(Locale.ENGLISH);
        String type;
        if ("http".equals(protocol)) {
            type = IProxyData.HTTP_PROXY_TYPE;
        } else if ("https".equals(protocol)) {
            type = IProxyData.HTTPS_PROXY_TYPE;
        } else if ("socks4".equals(protocol) || "socks_5".equals(protocol)) {
            type = IProxyData.SOCKS_PROXY_TYPE;
        } else {
            throw new IllegalArgumentException("unknown proxy protocol: " + protocol);
        }
        return type;
    }

    private void clearPersistentProxySettings() {
        Preferences netPreferences = ConfigurationScope.INSTANCE.getNode(Activator.ID);
        try {
            recursiveClear(netPreferences);
            netPreferences.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private static void recursiveClear(Preferences preferences) throws BackingStoreException {
        for (String child : preferences.childrenNames()) {
            recursiveClear(preferences.node(child));
        }
        preferences.clear();
    }

}
