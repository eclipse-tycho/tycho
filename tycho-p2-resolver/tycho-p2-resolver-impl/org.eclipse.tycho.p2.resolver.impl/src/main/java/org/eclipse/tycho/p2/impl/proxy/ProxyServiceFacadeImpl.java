/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.proxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.core.internal.net.Activator;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.tycho.p2.metadata.ProxyServiceFacade;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

@SuppressWarnings("restriction")
public class ProxyServiceFacadeImpl implements ProxyServiceFacade {
    private static final String MAVEN_SETTINGS_SOURCE = "MAVEN_SETTINGS";

    private static final Pattern NON_PROXY_DELIMITERS = Pattern.compile("\\s*[|,]\\s*");

    private IProxyService proxyService;

    public void configureProxy(String protocol, String host, int port, String user, String password,
            String nonProxyHosts) {
        try {
            ProxyData proxyData = new ProxyData(getProxyType(protocol));
            proxyData.setHost(host);
            proxyData.setPort(port);
            proxyData.setUserid(user);
            proxyData.setPassword(password);
            proxyData.setSource(MAVEN_SETTINGS_SOURCE);
            proxyService.setProxyData(new IProxyData[] { proxyData });
            if (nonProxyHosts != null && nonProxyHosts.trim().length() > 0) {
                proxyService.setNonProxiedHosts(NON_PROXY_DELIMITERS.split(nonProxyHosts.trim()));
            }
            // have to register authenticator manually as this is provided as extension point in
            // org.eclipse.ui.net only ...
            registerAuthenticator(user, password);
            proxyService.setProxiesEnabled(true);
            // disable the eclipse native proxy providers
            proxyService.setSystemProxiesEnabled(false);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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

    public void clearPersistentProxySettings() {
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

    public void setProxyServer(IProxyService proxyService) {
        this.proxyService = proxyService;
    }

    public void unsetProxyServer(IProxyService proxyService) {
        this.proxyService = null;
    }
}
