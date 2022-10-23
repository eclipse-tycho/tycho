/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.agent;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.core.shared.MavenContext;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager")
public class RemoteArtifactRepositoryManagerAgentFactory implements IAgentServiceFactory {

    @Requirement
    MavenContext mavenContext;

    @Override
    public Object createService(IProvisioningAgent agent) {
        IArtifactRepositoryManager plainRepoManager = (IArtifactRepositoryManager) new ArtifactRepositoryComponent()
                .createService(agent);
        if (getDisableP2MirrorsConfiguration()) {
            plainRepoManager = new P2MirrorDisablingArtifactRepositoryManager(plainRepoManager,
                    mavenContext.getLogger());
        }
        IRepositoryIdManager loadingHelper = agent.getService(IRepositoryIdManager.class);
        return new RemoteArtifactRepositoryManager(plainRepoManager, loadingHelper);
    }

    private boolean getDisableP2MirrorsConfiguration() {
        String key = "tycho.disableP2Mirrors";
        String value = mavenContext.getSessionProperties().getProperty(key);

        boolean disableP2Mirrors = Boolean.parseBoolean(value);
        if (disableP2Mirrors && mavenContext.getLogger().isDebugEnabled()) {
            String message = key + "=" + value + " -> ignoring mirrors specified in p2 artifact repositories";
            mavenContext.getLogger().debug(message);
        }
        return disableP2Mirrors;
    }
}
