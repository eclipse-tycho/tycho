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

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverService;
import org.eclipse.tycho.p2.target.TargetPlatformBuilderImpl;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.repository.gav.GAVArtifactLocator;
import org.eclipse.tycho.repository.gav.LocalRepositoryArtifactLocator;
import org.eclipse.tycho.repository.local.LocalArtifactRepository;
import org.eclipse.tycho.repository.local.LocalMetadataRepository;

public class P2ResolverFactoryImpl implements P2ResolverFactory {

    // TODO cache these instances in an p2 agent, and not here
    private static LocalMetadataRepository localMetadataRepository;
    private static LocalArtifactRepository localArtifactRepository;

    private MavenContext mavenContext;
    private LocalRepositoryP2Indices localRepoIndices;
    private RemoteAgentManager remoteAgentManager;
    private TargetDefinitionResolverService targetDefinitionResolverService;

    public TargetPlatformBuilderImpl createTargetPlatformBuilder(ExecutionEnvironmentConfiguration eeConfiguration) {
        IProvisioningAgent remoteAgent;
        try {
            remoteAgent = remoteAgentManager.getProvisioningAgent();
            LocalMetadataRepository localMetadataRepo = getLocalMetadataRepository(mavenContext, localRepoIndices);
            LocalArtifactRepository localArtifactRepo = getLocalArtifactRepository(mavenContext, localRepoIndices);
            ExecutionEnvironmentResolutionHandler eeResolutionHandler = ExecutionEnvironmentResolutionHandler
                    .adapt(eeConfiguration);
            return new TargetPlatformBuilderImpl(remoteAgent, mavenContext, targetDefinitionResolverService,
                    eeResolutionHandler, localArtifactRepo, localMetadataRepo);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized LocalMetadataRepository getLocalMetadataRepository(MavenContext context,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localMetadataRepository == null) {
            File localMavenRepoRoot = context.getLocalRepositoryRoot();
            GAVArtifactLocator contentLocator = new LocalRepositoryArtifactLocator(localMavenRepoRoot);
            localMetadataRepository = new LocalMetadataRepository(localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), contentLocator);

            // TODO this logic shouldn't be here
            String localArtifactHandling = context.getSessionProperties().getProperty("tycho.localArtifacts");
            if ("ignore".equalsIgnoreCase(localArtifactHandling)) {
                context.getLogger().debug(
                        "tycho.localArtifacts=" + localArtifactHandling + " -> ignoring locally built artifacts");
                localMetadataRepository.setIncludeInTargetPlatform(false);
            }
        }
        return localMetadataRepository;
    }

    private static synchronized LocalArtifactRepository getLocalArtifactRepository(MavenContext mavenContext,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localArtifactRepository == null) {
            GAVArtifactLocator contentLocator = new LocalRepositoryArtifactLocator(
                    mavenContext.getLocalRepositoryRoot());
            localArtifactRepository = new LocalArtifactRepository(localRepoIndices, contentLocator);
        }
        return localArtifactRepository;
    }

    public P2ResolverImpl createResolver(MavenLogger logger) {
        return new P2ResolverImpl(logger);
    }

    // setters for DS

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

    public void setLocalRepositoryIndices(LocalRepositoryP2Indices localRepoIndices) {
        this.localRepoIndices = localRepoIndices;
    }

    public void setRemoteAgentManager(RemoteAgentManager remoteAgentManager) {
        this.remoteAgentManager = remoteAgentManager;
    }

    public void setTargetDefinitionResolverService(TargetDefinitionResolverService targetDefinitionResolverService) {
        this.targetDefinitionResolverService = targetDefinitionResolverService;
    }
}
