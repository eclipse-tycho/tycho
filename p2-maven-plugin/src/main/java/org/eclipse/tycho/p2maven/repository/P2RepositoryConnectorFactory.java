/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christop Läubrich - Make P2ArtifactRepositoryLayout actually lookup artifacts
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import java.net.URI;
import java.net.URISyntaxException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.p2maven.LoggerProgressMonitor;

@Component(role = RepositoryConnectorFactory.class, hint = "p2")
public class P2RepositoryConnectorFactory implements RepositoryConnectorFactory {

	@Requirement
	private Logger log;

	@Requirement
	private IProvisioningAgent agent;

	@Requirement(hint = "maven2")
	private RepositoryLayoutFactory factory;

	@Override
	public float getPriority() {
		return 0;
	}

	@Override
	public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
			throws NoRepositoryConnectorException {
		if (P2ArtifactRepositoryLayout.ID.equals(repository.getContentType())) {
			URI uri;
			try {
				uri = new URI(repository.getUrl());
			} catch (URISyntaxException e) {
				log.error("Invalid URI '" + repository.getUrl() + ": " + e.getMessage(), e);
				throw new NoRepositoryConnectorException(repository, e);
			}
			RepositoryLayout repositoryLayout;
			try {
				repositoryLayout = factory.newInstance(session,
						new RemoteRepository.Builder(null, "default", null).build());
			} catch (NoRepositoryLayoutException e) {
				log.error("Can't create RepositoryLayout", e);
				throw new NoRepositoryConnectorException(repository, e);
			}
			try {
				IArtifactRepositoryManager repositoryManager = agent.getService(IArtifactRepositoryManager.class);
				IArtifactRepository artifactRepository = repositoryManager.loadRepository(uri,
						new LoggerProgressMonitor(log));
				return new P2RepositoryConnector(repository, artifactRepository, session, repositoryLayout,
						log);
			} catch (ProvisionException e) {
				log.error("Can't access repository at URI '" + repository.getUrl() + ": " + e.getStatus(), e);
				throw new NoRepositoryConnectorException(repository, e);
			}
		}
		throw new NoRepositoryConnectorException(repository);
	}

}
