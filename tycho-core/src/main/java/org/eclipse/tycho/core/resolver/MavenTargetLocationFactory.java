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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.core.MavenDependenciesResolver;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation;
import org.eclipse.tycho.targetplatform.TargetDefinitionContent;

@Component(role = MavenTargetLocationFactory.class)
public class MavenTargetLocationFactory {

    @Requirement
    SyncContextFactory syncContextFactory;

    @Requirement
    MavenContext mavenContext;

    @Requirement
    MavenDependenciesResolver dependenciesResolver;

    @Requirement
    IProvisioningAgent provisioningAgent;

    @Requirement
    RepositorySystem repositorySystem;

    @Requirement
    org.eclipse.aether.RepositorySystem repositorySystem2;

    @Requirement
    LegacySupport legacySupport;

    public TargetDefinitionContent resolveTargetDefinitionContent(MavenGAVLocation location,
            IncludeSourceMode includeSourceMode) {
        return new MavenTargetDefinitionContent(location, dependenciesResolver, includeSourceMode, provisioningAgent,
                mavenContext, syncContextFactory, repositorySystem, legacySupport.getSession(), repositorySystem2);
    }
}
