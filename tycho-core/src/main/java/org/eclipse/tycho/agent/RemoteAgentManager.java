/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.agent;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.osgi.TychoServiceFactory;

/**
 * Manager for {@link RemoteAgent} instances used to access remote p2 repositories. The instance are
 * shared within a reactor because they cache the loaded p2 repositories.
 */
@Component(role = RemoteAgentManager.class)
public class RemoteAgentManager {

    @Requirement
    private MavenContext mavenContext;
    @Requirement
    private MavenRepositorySettings mavenRepositorySettings;

    @Requirement(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory serviceFactory;

    @Requirement
    IProvisioningAgentProvider agentProvider;

    @Requirement
    IRepositoryIdManager repositoryIdManager;

    /**
     * Cached provisioning agent instance.
     */
    // TODO stop when this service is stopped?
    private IProvisioningAgent cachedAgent;
    private IProxyService proxyService;

    public RemoteAgentManager(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    // constructor for DS
    public RemoteAgentManager() {
    }

    public synchronized IProvisioningAgent getProvisioningAgent() throws ProvisionException {
        if (cachedAgent == null) {
            boolean disableP2Mirrors = getDisableP2MirrorsConfiguration();
            IProvisioningAgent createAgent = agentProvider.createAgent(null);
            createAgent.registerService(IRepositoryIdManager.SERVICE_NAME, repositoryIdManager);
            cachedAgent = new RemoteAgent(mavenContext, getProxyService(), mavenRepositorySettings, disableP2Mirrors,
                    createAgent);
        }
        return cachedAgent;
    }

    private IProxyService getProxyService() {
        if (proxyService == null) {
            proxyService = serviceFactory.getService(IProxyService.class);
        }
        return proxyService;
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

    public void setProxyService(IProxyService proxyService) {
        this.proxyService = proxyService;
    }

}
