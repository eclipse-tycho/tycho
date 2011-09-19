/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

/**
 * A facade for the org.eclipse.core.net.proxy.IProxyService which hides original eclipse classes
 * and thus enables usage from POM-first maven projects which can not have a dependency on
 * org.eclipse.core.net.
 */
public interface ProxyServiceFacade {

    /**
     * Configure the OSGI proxy service for the protocol specified. Parameter values are assumed to
     * be taken from {@see org.apache.maven.settings.Proxy}
     * 
     * @param protocol
     *            proxy protocol
     * @param host
     *            proxy host
     * @param port
     *            proxy port
     * @param user
     *            may be <code>null</code>
     * @param password
     *            may be <code>null</code>
     * @param nonProxyHosts
     *            pipe-separated list of non-proxied hosts, may be <code>null</code>
     */
    public void configureProxy(String protocol, String host, int port, String user, String password,
            String nonProxyHosts);

    /**
     * Discard persistent proxy settings. This is needed because
     * org.eclipse.core.net.proxy.IProxyService always remembers its settings in
     * eclipse/configuration/.settings/org.eclipse.core.net.prefs. Otherwise proxy settings would
     * survive across OSGi framework restarts and thus influence subsequent builds.
     */
    public void clearPersistentProxySettings();

}
