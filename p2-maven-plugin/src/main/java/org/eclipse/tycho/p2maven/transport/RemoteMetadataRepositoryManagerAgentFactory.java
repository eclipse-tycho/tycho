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
package org.eclipse.tycho.p2maven.transport;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.inject.Inject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;

@Named("org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager")
@Singleton
public class RemoteMetadataRepositoryManagerAgentFactory implements IAgentServiceFactory {

    @Inject
    Logger logger;

	@Inject
	IRepositoryIdManager repositoryIdManager;

	@Inject
	MavenAuthenticator mavenAuthenticator;

    @Override
    public Object createService(IProvisioningAgent agent) {
        IMetadataRepositoryManager plainMetadataRepoManager = (IMetadataRepositoryManager) new MetadataRepositoryComponent()
                .createService(agent);
		return new RemoteMetadataRepositoryManager(plainMetadataRepoManager, repositoryIdManager, logger,
				mavenAuthenticator);
    }

}
