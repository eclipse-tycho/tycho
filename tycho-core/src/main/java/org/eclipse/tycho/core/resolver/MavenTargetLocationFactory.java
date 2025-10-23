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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.LegacySupport;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;

@Named
@Singleton
public class MavenTargetLocationFactory {

    @Inject
    SyncContextFactory syncContextFactory;

    @Inject
    MavenContext mavenContext;

    @Inject
    MavenDependenciesResolver dependenciesResolver;

    @Inject
    IProvisioningAgent provisioningAgent;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    LegacySupport legacySupport;

    public TargetDefinitionContent resolveTargetDefinitionContent(MavenGAVLocation location,
            IncludeSourceMode includeSourceMode) {
        return new MavenTargetDefinitionContent(location, dependenciesResolver, includeSourceMode, provisioningAgent,
                mavenContext, syncContextFactory, legacySupport.getSession(), repositorySystem);
    }
}
