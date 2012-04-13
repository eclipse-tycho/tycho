/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - split off TargetPlatformBuilder
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.maven.repository.LocalArtifactRepository;
import org.eclipse.tycho.p2.maven.repository.LocalMetadataRepository;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.TargetPlatformBuilderImpl;

public class P2ResolverFactoryImpl implements P2ResolverFactory {

    // TODO cache these instances in an p2 agent, and not here
    private static LocalMetadataRepository localMetadataRepository;
    private static LocalArtifactRepository localArtifactRepository;

    private MavenContext mavenContext;
    private LocalRepositoryP2Indices localRepoIndices;
    private RemoteAgentManager remoteAgentManager;

    public TargetPlatformBuilderImpl createTargetPlatformBuilder(String bree, boolean disableP2Mirrors) {
        IProvisioningAgent remoteAgent;
        try {
            remoteAgent = remoteAgentManager.getProvisioningAgent(disableP2Mirrors);
            LocalMetadataRepository localMetadataRepo = getLocalMetadataRepository(
                    mavenContext.getLocalRepositoryRoot(), localRepoIndices);
            LocalArtifactRepository localArtifactRepo = getLocalArtifactRepository(
                    mavenContext.getLocalRepositoryRoot(), localRepoIndices);
            return new TargetPlatformBuilderImpl(remoteAgent, mavenContext, bree, localArtifactRepo, localMetadataRepo);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized LocalMetadataRepository getLocalMetadataRepository(File localMavenRepoRoot,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localMetadataRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(localMavenRepoRoot);
            localMetadataRepository = new LocalMetadataRepository(localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), contentLocator);
        }
        return localMetadataRepository;
    }

    private static synchronized LocalArtifactRepository getLocalArtifactRepository(File localMavenRepoRoot,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localArtifactRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(localMavenRepoRoot);
            localArtifactRepository = new LocalArtifactRepository(localRepoIndices, contentLocator);
        }
        return localArtifactRepository;
    }

    public P2ResolverImpl createResolver(MavenLogger logger) {
        return new P2ResolverImpl(logger);
    }

    // --------------

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setLocalRepositoryIndices(LocalRepositoryP2Indices localRepoIndices) {
        this.localRepoIndices = localRepoIndices;
    }

    public void setRemoteAgentManager(RemoteAgentManager remoteAgentManager) {
        this.remoteAgentManager = remoteAgentManager;
    }

}
