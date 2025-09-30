/*******************************************************************************
 * Copyright (c) 2022, 2025 Christoph Läubrich and others.
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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Repository;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.tycho.IRepositoryIdManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.eclipse.tycho.p2maven.ListCompositeArtifactRepository;
import org.eclipse.tycho.p2maven.ListQueryable;
import org.eclipse.tycho.p2maven.LoggerProgressMonitor;

/**
 * Allows unique access to P2 repositories from maven
 */
@Singleton
public class P2RepositoryManager {
	private static final String PROPERTY_KEY = "eclipse.p2.maxDownloadAttempts";

	@Inject
	MavenPropertyHelper propertyHelper;

	@Inject
	IRepositoryIdManager repositoryIdManager;

	@Inject
	IProvisioningAgent agent;

	@Inject
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
	public IArtifactRepository getArtifactRepository(MavenRepositoryLocation repository) throws ProvisionException {
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

	public IArtifactRepository createLocalArtifactRepository(Path location, String name, Map<String, String> properties)
			throws ProvisionException {
		ArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		factory.setAgent(agent);
		return factory.create(location.toUri(), name, null, properties);
	}

	/**
	 * Returns all artifacts contained in the {@link IArtifactRepository} at the
	 * given location. This method does NOT check the type of the repository! <br>
	 * This can be useful to obtain overview information and can serve as existence
	 * check for a repository.
	 * 
	 * @param location the location of the repository to load
	 * @param id       the optional id of the repository (e.g. used for
	 *                 authentication), may be {@code null}
	 * @return the number of contained artifacts
	 * @throws ProvisionException if loading the repository failed
	 */
	public IQueryResult<IArtifactKey> allArtifacts(URI location, String id) throws ProvisionException {
		IArtifactRepository repository = getArtifactRepository(location, id);
		return repository.query(ArtifactKeyQuery.ALL_KEYS, null);
	}

	/**
	 * Creates a local mirror only of the <b>data</b> of the given
	 * {@link IMetadataRepository}, i.e. its {@code artifacts.xml/jar/xml.xz} files.
	 */
	public IArtifactRepository mirrorArtifactRepositoryData(IArtifactRepository repository, Path targetLocation)
			throws ProvisionException {
		IArtifactRepository mirror = createLocalArtifactRepository(targetLocation, repository.getName(),
				repository.getProperties());
		IStatus result = mirror.executeBatch(m -> {
			// Copy artifacts to modifiable output repository
			repository.query(ArtifactKeyQuery.ALL_KEYS, null).stream().map(a -> repository.getArtifactDescriptors(a))
					.forEach(a -> mirror.addDescriptors(a, null));
			// Copy (potentially customized) mapping rules
			if (repository instanceof SimpleArtifactRepository simpleSource
					&& mirror instanceof SimpleArtifactRepository simpleTarget) {
				simpleTarget.setRules(simpleSource.getRules()); // Copy artifact mapping rules
			}
		}, null);
		assertNoError(result);
		return mirror;
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
	public IMetadataRepository getMetadataRepository(MavenRepositoryLocation repository) throws ProvisionException {
		return getMetadataRepositor(repository.getURL(), repository.getId());
	}

	public IMetadataRepository createLocalMetadataRepository(Path location, String name, Map<String, String> properties)
			throws ProvisionException {
		MetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		factory.setAgent(agent);
		return factory.create(location.toUri(), name, null, properties);
	}

	/**
	 * Returns all units contained in the {@link IMetadataRepository} at the given
	 * location. This method does NOT check the type of the repository! <br>
	 * This can be useful to obtain overview information and can serve as existence
	 * check for a repository.
	 * 
	 * @param location the location of the repository to load
	 * @param id       the optional id of the repository (e.g. used for
	 *                 authentication), may be {@code null}
	 * @return the number of contained units
	 * @throws ProvisionException if loading the repository failed
	 */
	public IQueryResult<IInstallableUnit> allMetadataUnits(URI location, String id) throws ProvisionException {
		IMetadataRepository repository = getMetadataRepositor(location, id);
		return repository.query(QueryUtil.ALL_UNITS, null);
	}

	/** Creates a local mirror of the given {@link IMetadataRepository}. */
	public IMetadataRepository mirrorMetadataRepository(IMetadataRepository repository, Path targetLocation)
			throws ProvisionException {
		IMetadataRepository mirror = createLocalMetadataRepository(targetLocation, repository.getName(),
				repository.getProperties());
		IStatus result = mirror.executeBatch(m -> {
			// Copy units to modifiable output repository
			repository.query(QueryUtil.ALL_UNITS, null).stream().map(List::of).forEach(mirror::addInstallableUnits);
		}, null);
		assertNoError(result);
		return mirror;
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

	public static void assertNoError(IStatus status) throws ProvisionException {
		if (status.matches(IStatus.ERROR)) {
			throw new ProvisionException(status);
		}
	}

	public void downloadArtifact(IInstallableUnit iu, IArtifactRepository artifactRepository, OutputStream outputStream)
			throws IOException {
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
