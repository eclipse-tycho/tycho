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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.remote.RemoteAgent;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.TargetPlatformBuilderImpl;

public class P2ResolverFactoryImpl implements P2ResolverFactory {

    private static IProvisioningAgent cachedAgent;
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

    public static synchronized IProvisioningAgent getProvisioningAgent(MavenContext mavenContext) {
        if (cachedAgent == null) {
            try {
                cachedAgent = new RemoteAgent(mavenContext);
            } catch (ProvisionException e) {
                throw new RuntimeException(e);
            }
        }
        return cachedAgent;
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
        cachedAgent = null;
    }
}
