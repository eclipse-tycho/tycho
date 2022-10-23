/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho.agent;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.MavenRepositorySettings;
import org.eclipse.tycho.core.shared.MavenContext;

@SuppressWarnings("restriction")
public class RemoteAgent implements IProvisioningAgent {

    private IProvisioningAgent delegate;

    public RemoteAgent(MavenContext mavenContext, IProxyService proxyService,
            MavenRepositorySettings mavenRepositorySettings, boolean disableMirrors, IProvisioningAgent baseAgent)
            throws ProvisionException {
        this.delegate = createConfiguredProvisioningAgent(mavenContext, proxyService, disableMirrors,
                mavenRepositorySettings, baseAgent);
    }

    // constructor for tests
    public RemoteAgent(MavenContext mavenContext, boolean disableP2Mirrors, IProvisioningAgent baseAgent)
            throws ProvisionException {
        this(mavenContext, null, null, disableP2Mirrors, baseAgent);
    }

    // constructor for tests
    public RemoteAgent(MavenContext mavenContext, IProvisioningAgent baseAgent) throws ProvisionException {
        this(mavenContext, null, null, false, baseAgent);
    }

    private static IProvisioningAgent createConfiguredProvisioningAgent(MavenContext mavenContext,
            IProxyService proxyService, boolean disableP2Mirrors, MavenRepositorySettings mavenRepositorySettings,
            IProvisioningAgent baseAgent) throws ProvisionException {
        AgentBuilder agent = new AgentBuilder(baseAgent);

        return agent.getAgent();
    }


    /**
     * Wrapper around an {@link IProvisioningAgent} with type-safe service access.
     */
    static class AgentBuilder {

        private final IProvisioningAgent wrappedAgent;

        public AgentBuilder(IProvisioningAgent wrappedAgent) {
            this.wrappedAgent = wrappedAgent;
        }

        public <T> T getService(Class<T> type) {
            return type.cast(wrappedAgent.getService(type.getName()));
        }

        public <T> void registerService(Class<T> type, T instance) {
            wrappedAgent.registerService(type.getName(), instance);
        }

        public IProvisioningAgent getAgent() {
            return wrappedAgent;
        }

    }

    // end initialization

    @Override
    public Object getService(String serviceName) {
        return delegate.getService(serviceName);
    }

    @Override
    public void registerService(String serviceName, Object service) {
        delegate.registerService(serviceName, service);
    }

    public <T> T getService(Class<T> type) {
        return type.cast(getService(type.getName()));
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void unregisterService(String serviceName, Object service) {
        delegate.unregisterService(serviceName, service);
    }

}
