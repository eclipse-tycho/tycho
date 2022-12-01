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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.maven.model.Repository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.p2maven.LoggerProgressMonitor;

/**
 * Allows unique access to P2 repositories from maven
 */
@Component(role = P2RepositoryManager.class)
public class P2RepositoryManager {

	@Requirement
	IRepositoryIdManager repositoryIdManager;

	@Requirement
	IProvisioningAgent agent;

	@Requirement
	Logger logger;

	/**
	 * Loads the {@link IArtifactRepository} from the given {@link Repository}, this
	 * method does NOT check the type of the repository!
	 * 
	 * @param repository
	 * @return the {@link IArtifactRepository} for the given {@link Repository}
	 * @throws URISyntaxException if {@link Repository#getUrl()} can not be
	 *                            converted into an {@link URI}
	 * @throws ProvisionException if loading the repository failed
	 */
	public IArtifactRepository getArtifactRepository(Repository repository)
			throws URISyntaxException, ProvisionException {
		return getArtifactRepository(new URI(repository.getUrl()), repository.getId());
	}

	/**
	 * Loads the {@link IArtifactRepository} from the given {@link Repository}, this
	 * method does NOT check the type of the repository!
	 * 
	 * @param repository
	 * @return the {@link IArtifactRepository} for the given {@link Repository}
	 * @throws ProvisionException if loading the repository failed
	 */
	public IArtifactRepository getArtifactRepository(MavenRepositoryLocation repository)
			throws ProvisionException {
		return getArtifactRepository(repository.getURL(), repository.getId());
	}

	/**
	 * Loads the {@link IMetadataRepository} from the given {@link Repository}, this
	 * method does NOT check the type of the repository!
	 * 
	 * @param repository
	 * @return the {@link IMetadataRepository} for the given {@link Repository}
	 * @throws URISyntaxException if {@link Repository#getUrl()} can not be
	 *                            converted into an {@link URI}
	 * @throws ProvisionException if loading the repository failed
	 */
	public IMetadataRepository getMetadataRepositor(Repository repository)
			throws URISyntaxException, ProvisionException {
		return getMetadataRepositor(new URI(repository.getUrl()), repository.getId());
	}

	/**
	 * Loads the {@link IMetadataRepository} from the given {@link Repository}, this
	 * method does NOT check the type of the repository!
	 * 
	 * @param repository
	 * @return the {@link IMetadataRepository} for the given {@link Repository}
	 * @throws ProvisionException if loading the repository failed
	 */
	public IMetadataRepository getMetadataRepositor(MavenRepositoryLocation repository)
			throws ProvisionException {
		return getMetadataRepositor(repository.getURL(), repository.getId());
	}

	private IArtifactRepository getArtifactRepository(URI location, String id) throws ProvisionException {
		repositoryIdManager.addMapping(id, location);
		IArtifactRepositoryManager repositoryManager = agent.getService(IArtifactRepositoryManager.class);
		return repositoryManager.loadRepository(location, new LoggerProgressMonitor(logger));
	}

	private IMetadataRepository getMetadataRepositor(URI location, String id) throws ProvisionException {
		repositoryIdManager.addMapping(id, location);
		IMetadataRepositoryManager metadataManager = agent.getService(IMetadataRepositoryManager.class);
		return metadataManager.loadRepository(location, new LoggerProgressMonitor(logger));
	}

	public void downloadArtifact(IInstallableUnit iu, IArtifactRepository artifactRepository,
			OutputStream outputStream) throws IOException {
		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		for (IArtifactKey key : artifacts) {
			IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
			for (IArtifactDescriptor descriptor : descriptors) {
				IStatus status = artifactRepository.getRawArtifact(descriptor, outputStream,
						new LoggerProgressMonitor(logger));
				if (status.isOK()) {
					return;
				}
				throw new IOException("Download failed: " + status);
			}
		}
		throw new FileNotFoundException();
	}

}
