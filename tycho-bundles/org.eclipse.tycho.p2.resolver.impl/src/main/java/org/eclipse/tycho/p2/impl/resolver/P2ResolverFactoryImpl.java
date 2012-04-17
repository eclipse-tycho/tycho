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
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.remote.RemoteAgent;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.TargetPlatformBuilderImpl;

public class P2ResolverFactoryImpl implements P2ResolverFactory {

    private MavenContext mavenContext;
    private LocalRepositoryP2Indices localRepoIndices;

    public TargetPlatformBuilderImpl createTargetPlatformBuilder(String bree, boolean disableP2Mirrors) {
        IProvisioningAgent agent = getProvisioningAgent(mavenContext);
        return new TargetPlatformBuilderImpl(agent, mavenContext, bree, localRepoIndices, disableP2Mirrors);
    }

    public P2ResolverImpl createResolver(MavenLogger logger) {
        return new P2ResolverImpl(logger);
    }

    // --------------

    /**
     * IProvisioningAgent's must be long-lived because they keep instances of tycho repository
     * cache. Cache instances are parametrized by local repository path and offline mode and thus
     * different agent must be used for each (localrepo,offline) combination.
     * 
     * @TODO move to activator and stop all agents during Bundle#stop
     */
    private static final Map<AgentKey, IProvisioningAgent> agents = new HashMap<P2ResolverFactoryImpl.AgentKey, IProvisioningAgent>();

    private static class AgentKey {
        public final File localMavenRepositoryRoot;
        public final boolean offline;

        public AgentKey(File localMavenRepositoryRoot, boolean offline) {
            this.localMavenRepositoryRoot = localMavenRepositoryRoot;
            this.offline = offline;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + localMavenRepositoryRoot.hashCode();
            hash = hash * 31 + (offline ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AgentKey)) {
                return false;
            }
            AgentKey other = (AgentKey) obj;
            return localMavenRepositoryRoot.equals(other.localMavenRepositoryRoot) && offline == other.offline;
        }
    }

    public static synchronized IProvisioningAgent getProvisioningAgent(MavenContext mavenContext) {
        AgentKey agentKey = new AgentKey(mavenContext.getLocalRepositoryRoot(), mavenContext.isOffline());
        IProvisioningAgent agent = agents.get(agentKey);
        if (agent == null) {
            try {
                agent = new RemoteAgent(mavenContext);

                agents.put(agentKey, agent);
            } catch (ProvisionException e) {
                throw new RuntimeException(e);
            }
        }
        return agent;
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setLocalRepositoryIndices(LocalRepositoryP2Indices localRepoIndices) {
        this.localRepoIndices = localRepoIndices;
    }

    /**
     * This method is meant for use by tests to purge any cache state between test invocations
     */
    public static void purgeAgents() {
        agents.clear();
    }
}
