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

import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.p2.impl.Activator;

@SuppressWarnings("restriction")
public class RemoteAgent implements IProvisioningAgent {

    private IProvisioningAgent delegate;

    public RemoteAgent(MavenContext mavenContext, boolean disableP2Mirrors) throws ProvisionException {
        this.delegate = createConfiguredProvisioningAgent(mavenContext, disableP2Mirrors);
    }

    public RemoteAgent(MavenContext mavenContext) throws ProvisionException {
        this(mavenContext, false);
    }

    private static IProvisioningAgent createConfiguredProvisioningAgent(MavenContext mavenContext,
            boolean disableP2Mirrors) throws ProvisionException {
        // TODO set a temporary folder as persistence location
        AgentBuilder agent = new AgentBuilder(Activator.newProvisioningAgent());

        // suppress p2.index access
        final Transport transport;
        if (mavenContext.isOffline()) {
            transport = new OfflineTransport();
            agent.registerService(Transport.class, transport);
        } else {
            transport = agent.getService(Transport.class);
        }

        // cache indices of p2 repositories in the local Maven repository
        TychoP2RepositoryCacheManager cacheMgr = new TychoP2RepositoryCacheManager(transport, mavenContext);
        agent.registerService(CacheManager.class, cacheMgr);

        if (disableP2Mirrors) {
            addP2MirrorDisablingRepositoryManager(agent);
        }

        return agent.getAgent();
    }

    private static void addP2MirrorDisablingRepositoryManager(AgentBuilder agent) {
        // wrap artifact repository manager
        IArtifactRepositoryManager plainRepoManager = agent.getService(IArtifactRepositoryManager.class);
        IArtifactRepositoryManager mirrorDisablingRepoManager = new P2MirrorDisablingArtifactRepositoryManager(
                plainRepoManager);
        agent.registerService(IArtifactRepositoryManager.class, mirrorDisablingRepoManager);
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

    public Object getService(String serviceName) {
        return delegate.getService(serviceName);
    }

    public void registerService(String serviceName, Object service) {
        delegate.registerService(serviceName, service);
    }

    public <T> T getService(Class<T> type) {
        return type.cast(getService(type.getName()));
    }

    public void stop() {
        delegate.stop();
    }

    public void unregisterService(String serviceName, Object service) {
        delegate.unregisterService(serviceName, service);
    }

}
