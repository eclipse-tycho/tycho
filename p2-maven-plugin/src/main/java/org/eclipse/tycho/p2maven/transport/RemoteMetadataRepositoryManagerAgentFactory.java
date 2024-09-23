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

import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryComponent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager")
public class RemoteMetadataRepositoryManagerAgentFactory implements IAgentServiceFactory {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private final IRepositoryIdManager repositoryIdManager;
	private final MavenAuthenticator mavenAuthenticator;

    @Inject
    public RemoteMetadataRepositoryManagerAgentFactory(IRepositoryIdManager repositoryIdManager, MavenAuthenticator mavenAuthenticator) {
        this.repositoryIdManager = repositoryIdManager;
        this.mavenAuthenticator = mavenAuthenticator;
    }

    @Override
    public Object createService(IProvisioningAgent agent) {
        IMetadataRepositoryManager plainMetadataRepoManager = (IMetadataRepositoryManager) new MetadataRepositoryComponent()
                .createService(agent);
		return new RemoteMetadataRepositoryManager(plainMetadataRepoManager, repositoryIdManager, logger,
				mavenAuthenticator);
    }

}
