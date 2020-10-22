/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 567098 - pomDependencies=consider should wrap non-osgi jars
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.remote.RemoteAgentManager;
import org.eclipse.tycho.p2.repository.LocalRepositoryP2Indices;
import org.eclipse.tycho.p2.repository.LocalRepositoryReader;
import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.PomDependencyCollectorImpl;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverService;
import org.eclipse.tycho.p2.target.TargetPlatformFactoryImpl;
import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
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

    private static synchronized LocalMetadataRepository getLocalMetadataRepository(MavenContext context,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localMetadataRepository == null) {
            File localMavenRepoRoot = context.getLocalRepositoryRoot();
            RepositoryReader contentLocator = new LocalRepositoryReader(localMavenRepoRoot);
            localMetadataRepository = new LocalMetadataRepository(localMavenRepoRoot.toURI(),
                    localRepoIndices.getMetadataIndex(), contentLocator);

        }
        return localMetadataRepository;
    }

    private static synchronized LocalArtifactRepository getLocalArtifactRepository(MavenContext mavenContext,
            LocalRepositoryP2Indices localRepoIndices) {
        if (localArtifactRepository == null) {
            RepositoryReader contentLocator = new LocalRepositoryReader(mavenContext.getLocalRepositoryRoot());
            localArtifactRepository = new LocalArtifactRepository(localRepoIndices, contentLocator);
        }
        return localArtifactRepository;
    }

    @Override
    public PomDependencyCollector newPomDependencyCollector(ReactorProject project) {
        return new PomDependencyCollectorImpl(mavenContext, project);
    }

    @Override
    public TargetPlatformFactoryImpl getTargetPlatformFactory() {
        try {
            // TODO don't synchronize twice
            LocalMetadataRepository localMetadataRepo = getLocalMetadataRepository(mavenContext, localRepoIndices);
            LocalArtifactRepository localArtifactRepo = getLocalArtifactRepository(mavenContext, localRepoIndices);
            return new TargetPlatformFactoryImpl(mavenContext, remoteAgentManager.getProvisioningAgent(),
                    localArtifactRepo, localMetadataRepo, targetDefinitionResolverService);
        } catch (ProvisionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public P2ResolverImpl createResolver(MavenLogger logger) {
        return new P2ResolverImpl(getTargetPlatformFactory(), logger);
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
