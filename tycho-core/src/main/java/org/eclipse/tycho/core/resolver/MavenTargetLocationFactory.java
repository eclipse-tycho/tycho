/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class MavenTargetLocationFactory {

    private final SyncContextFactory syncContextFactory;
    private final MavenContext mavenContext;
    private final MavenDependenciesResolver dependenciesResolver;
    private final IProvisioningAgent provisioningAgent;
    private final RepositorySystem repositorySystem;
    private final org.eclipse.aether.RepositorySystem repositorySystem2;
    private final LegacySupport legacySupport;

    @Inject
    public MavenTargetLocationFactory(SyncContextFactory syncContextFactory, MavenContext mavenContext, MavenDependenciesResolver dependenciesResolver, IProvisioningAgent provisioningAgent, RepositorySystem repositorySystem, org.eclipse.aether.RepositorySystem repositorySystem2, LegacySupport legacySupport) {
        this.syncContextFactory = syncContextFactory;
        this.mavenContext = mavenContext;
        this.dependenciesResolver = dependenciesResolver;
        this.provisioningAgent = provisioningAgent;
        this.repositorySystem = repositorySystem;
        this.repositorySystem2 = repositorySystem2;
        this.legacySupport = legacySupport;
    }

    public TargetDefinitionContent resolveTargetDefinitionContent(MavenGAVLocation location,
                                                                  IncludeSourceMode includeSourceMode) {
        return new MavenTargetDefinitionContent(location, dependenciesResolver, includeSourceMode, provisioningAgent,
                mavenContext, syncContextFactory, repositorySystem, legacySupport.getSession(), repositorySystem2);
    }
}
