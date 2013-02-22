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
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings;
import org.eclipse.tycho.p2.impl.Activator;

@SuppressWarnings("restriction")
public class RemoteAgent implements IProvisioningAgent {

    private IProvisioningAgent delegate;

    public RemoteAgent(MavenContext mavenContext, MavenRepositorySettings mavenRepositorySettings,
            boolean disableMirrors) throws ProvisionException {
        this.delegate = createConfiguredProvisioningAgent(mavenContext, disableMirrors, mavenRepositorySettings);
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
            boolean disableP2Mirrors, MavenRepositorySettings mavenRepositorySettings) throws ProvisionException {
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
        RemoteRepositoryCacheManager cacheMgr = new RemoteRepositoryCacheManager(transport, mavenContext);
        agent.registerService(CacheManager.class, cacheMgr);

        if (disableP2Mirrors) {
            addP2MirrorDisablingRepositoryManager(agent, mavenContext.getLogger());
        }

        if (mavenRepositorySettings != null) {
            addMavenAwareRepositoryManagers(agent, mavenRepositorySettings, mavenContext.getLogger());
        }

        return agent.getAgent();
    }

    private static void addP2MirrorDisablingRepositoryManager(AgentBuilder agent, MavenLogger mavenLogger) {
        // wrap artifact repository manager
        IArtifactRepositoryManager plainRepoManager = agent.getService(IArtifactRepositoryManager.class);
        IArtifactRepositoryManager mirrorDisablingRepoManager = new P2MirrorDisablingArtifactRepositoryManager(
                plainRepoManager, mavenLogger);
        agent.registerService(IArtifactRepositoryManager.class, mirrorDisablingRepoManager);
    }

    private static void addMavenAwareRepositoryManagers(AgentBuilder agent,
            MavenRepositorySettings mavenRepositorySettings, MavenLogger logger) {

        // register service which stores mapping between URLs and IDs (used by Maven)
        RemoteRepositoryLoadingHelper loadingHelper = new RemoteRepositoryLoadingHelper(mavenRepositorySettings, logger);
        agent.registerService(IRepositoryIdManager.class, loadingHelper);

        // wrap metadata repository manager
        IMetadataRepositoryManager plainMetadataRepoManager = agent.getService(IMetadataRepositoryManager.class);
        IMetadataRepositoryManager remoteMetadataRepoManager = new RemoteMetadataRepositoryManager(
                plainMetadataRepoManager, loadingHelper);
        agent.registerService(IMetadataRepositoryManager.class, remoteMetadataRepoManager);

        // wrap artifact repository manager
        IArtifactRepositoryManager plainArtifactRepoManager = agent.getService(IArtifactRepositoryManager.class);
        RemoteArtifactRepositoryManager remoteArtifactRepoManager = new RemoteArtifactRepositoryManager(
                plainArtifactRepoManager, loadingHelper);
        agent.registerService(IArtifactRepositoryManager.class, remoteArtifactRepoManager);
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
