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
package org.eclipse.tycho.p2.remote;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.impl.Activator;

@SuppressWarnings("restriction")
public class RemoteAgent implements IProvisioningAgent {

    private IProvisioningAgent delegate;

    public RemoteAgent(MavenContext mavenContext, IProxyService proxyService, boolean disableMirrors)
            throws ProvisionException {
        this.delegate = createConfiguredProvisioningAgent(mavenContext, proxyService, disableMirrors);
    }

    // constructor for tests
    RemoteAgent(MavenContext mavenContext, boolean disableP2Mirrors) throws ProvisionException {
        this(mavenContext, null, disableP2Mirrors);
    }

    // constructor for tests
    public RemoteAgent(MavenContext mavenContext) throws ProvisionException {
        this(mavenContext, null, false);
    }

    private static IProvisioningAgent createConfiguredProvisioningAgent(MavenContext mavenContext,
            IProxyService proxyService, boolean disableP2Mirrors) throws ProvisionException {
        // TODO set a temporary folder as persistence location
        IProvisioningAgent provisioningAgent = Activator.newProvisioningAgent();
        AgentBuilder agent = new AgentBuilder(provisioningAgent);
        if ("ecf".equalsIgnoreCase(System.getProperty("tycho.p2.transport"))) {
            // suppress p2.index access
            final Transport transport;
            if (mavenContext.isOffline()) {
                transport = new OfflineTransport(mavenContext);
            } else {
                transport = new org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport(provisioningAgent);
            }
            agent.registerService(Transport.class, transport);
            // cache indices of p2 repositories in the local Maven repository
            RemoteRepositoryCacheManager cacheMgr = new RemoteRepositoryCacheManager(transport, mavenContext);
            agent.registerService(CacheManager.class, cacheMgr);
        }

        if (disableP2Mirrors) {
            addP2MirrorDisablingRepositoryManager(agent, mavenContext.getLogger());
        }

        makeCompositeRepositoryLoadingAtomicByDefault();

        return agent.getAgent();
    }

    private static void addP2MirrorDisablingRepositoryManager(AgentBuilder agent, MavenLogger mavenLogger) {
        // wrap artifact repository manager
        IArtifactRepositoryManager plainRepoManager = agent.getService(IArtifactRepositoryManager.class);
        IArtifactRepositoryManager mirrorDisablingRepoManager = new P2MirrorDisablingArtifactRepositoryManager(
                plainRepoManager, mavenLogger);
        agent.registerService(IArtifactRepositoryManager.class, mirrorDisablingRepoManager);
    }

    private static void makeCompositeRepositoryLoadingAtomicByDefault() {
        /*
         * Workaround for p2 bug 356561: Due to historical reasons, p2 considers a composite
         * repository to be loaded successfully even though some of its children failed to load.
         * This is bad for Tycho because it allows for network/server outages to threaten build
         * reproducibility. Therefore, we change the composite loading behaviour to be atomic for
         * composite repositories (except those that explicitly state
         * p2.atomic.composite.loading=false in their repository properties). This can be done via a
         * system property (see CompositeArtifactRepository and CompositeMetadataRepository).
         */
        String atomicDefaultSystemProperty = "eclipse.p2.atomic.composite.loading.default";

        if (System.getProperty(atomicDefaultSystemProperty) == null) {
            // not explicitly set on command line -> set Tycho's default
            System.setProperty(atomicDefaultSystemProperty, Boolean.toString(true));
        }
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
