/*******************************************************************************
 * Copyright (c) 2022, 2024 Christoph Läubrich and others.
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
import java.util.ArrayList;
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
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2maven.ListQueryable;
import org.eclipse.tycho.p2maven.LoggerProgressMonitor;

/**
 * Allows unique access to P2 repositories from maven
 */
@Component(role = P2RepositoryManager.class)
public class P2RepositoryManager {
	private static final String PROPERTY_KEY = "eclipse.p2.maxDownloadAttempts";

	@Requirement
	MavenPropertyHelper propertyHelper;

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

	public IArtifactRepository getCompositeArtifactRepository(Collection<Repository> repositories)
			throws ProvisionException, URISyntaxException {
		if (repositories.size() == 1) {
			return getArtifactRepository(repositories.iterator().next());
		}
		ArrayList<IArtifactRepository> childs = new ArrayList<>();
		for (Repository repository : repositories) {
			childs.add(getArtifactRepository(repository));
		}
		return new ListCompositeArtifactRepository(childs, agent);
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
	public IMetadataRepository getMetadataRepository(Repository repository)
			throws URISyntaxException, ProvisionException {
		return getMetadataRepositor(new URI(repository.getUrl()), repository.getId());
	}

	public IQueryable<IInstallableUnit> getCompositeMetadataRepository(Collection<Repository> repositories)
			throws ProvisionException, URISyntaxException {
		if (repositories.size() == 1) {
			return getMetadataRepository(repositories.iterator().next());
		}
		ListQueryable<IInstallableUnit> queryable = new ListQueryable<>();
		for (Repository repository : repositories) {
			queryable.add(getMetadataRepository(repository));
		}
		return queryable;
	}

	/**
	 * Loads the {@link IMetadataRepository} from the given {@link Repository}, this
	 * method does NOT check the type of the repository!
	 * 
	 * @param repository
	 * @return the {@link IMetadataRepository} for the given {@link Repository}
	 * @throws ProvisionException if loading the repository failed
	 */
	public IMetadataRepository getMetadataRepository(MavenRepositoryLocation repository)
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
		int maxDownloadAttempts = getMaxDownloadAttempts();
		
		for (IArtifactKey key : artifacts) {
			IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
			for (IArtifactDescriptor descriptor : descriptors) {
				for (int downloadAttempts = 0; downloadAttempts < maxDownloadAttempts; ++downloadAttempts) {
					IStatus status = artifactRepository.getRawArtifact(descriptor, outputStream,
							new LoggerProgressMonitor(logger));
					if (status.isOK()) {
						return;
					}
					// Might happen if e.g. a bad mirror was used
					if (status.getCode() == IArtifactRepository.CODE_RETRY) {
						logger.warn("Artifact repository requested retry (attempt [%d/%d]): '%s'"
								.formatted(downloadAttempts + 1, maxDownloadAttempts, status));
						continue;
					}
					throw new IOException("Download failed: " + status);
				}
			}
		}
		throw new FileNotFoundException();
	}

	private int getMaxDownloadAttempts() {
		String property = propertyHelper.getGlobalProperty(PROPERTY_KEY, "3");
		try {
			int maxDownloadAttempts = Integer.valueOf(property);
			if (maxDownloadAttempts <= 0) {
				logger.error("Value '%s' for property '%s', is not a positive number! Use 1 as default value."
						.formatted(property, PROPERTY_KEY));
				return 1;
			}
			return maxDownloadAttempts;
		} catch (NumberFormatException e) {
			logger.error("Value '%s' for property '%s', is not a number! Use 1 as default value.".formatted(property,
					PROPERTY_KEY));
			return 1;
		}
	}
}
