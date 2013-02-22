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
package org.eclipse.tycho.p2.remote;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;

/**
 * Manager for {@link RemoteAgent} instances used to access remote p2 repositories. The instance are
 * shared within a reactor because they cache the loaded p2 repositories.
 */
public class RemoteAgentManager {

    private MavenContext mavenContext;

    private MavenRepositorySettings mavenRepositorySettings;

    /**
     * Cached provisioning agent instance.
     */
    // TODO stop when this service is stopped?
    private IProvisioningAgent cachedAgent;

    public RemoteAgentManager(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    // constructor for DS
    public RemoteAgentManager() {
    }

    public synchronized IProvisioningAgent getProvisioningAgent() throws ProvisionException {
        if (cachedAgent == null) {
            boolean disableP2Mirrors = getDisableP2MirrorsConfiguration();
            cachedAgent = new RemoteAgent(mavenContext, mavenRepositorySettings, disableP2Mirrors);
        }
        return cachedAgent;
    }

    private boolean getDisableP2MirrorsConfiguration() {
        String key = "tycho.disableP2Mirrors";
        String value = mavenContext.getSessionProperties().getProperty(key);

        boolean disableP2Mirrors = Boolean.parseBoolean(value);
        if (disableP2Mirrors && mavenContext.getLogger().isDebugEnabled()) {
            String message = key + "=" + value + " -> ignoring mirrors specified in p2 artifact repositories";
            mavenContext.getLogger().debug(message);
        }
        return disableP2Mirrors;
    }

    // setters for DS
    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setMavenRepositorySettings(MavenRepositorySettings mavenRepositorySettings) {
        this.mavenRepositorySettings = mavenRepositorySettings;
    }

}
