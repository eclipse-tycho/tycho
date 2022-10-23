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
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;

@Component(role = IAgentServiceFactory.class, hint = "org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager")
public class RemoteMetadataRepositoryManagerAgentFactory implements IAgentServiceFactory {

    @Requirement
    Logger logger;

    @Override
    public Object createService(IProvisioningAgent agent) {
        IMetadataRepositoryManager plainMetadataRepoManager = (IMetadataRepositoryManager) new MetadataRepositoryComponent()
                .createService(agent);
        IRepositoryIdManager loadingHelper = agent.getService(IRepositoryIdManager.class);
        return new RemoteMetadataRepositoryManager(plainMetadataRepoManager, loadingHelper,
                new MavenLoggerAdapter(logger, true));
    }

}
