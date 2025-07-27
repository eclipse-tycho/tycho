/*******************************************************************************
 * Copyright (c) 2025, 2025 Hannes Wellmann and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SequencedSet;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Repository;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryIO;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.XZCompressor;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.p2maven.repository.P2RepositoryKind;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

@Named
@Singleton
public class P2RepositoryDataManipulator {

	public static class RepositoryLocationURISyntaxException extends Exception {
		public RepositoryLocationURISyntaxException(Exception cause) {
			super(cause);
		}
	}

	public static ModifiedRepositoryDescriptor createDescriptor(Repository repository, P2RepositoryKind repositoryKind,
			Path outputLocation, boolean compress, boolean xzCompress, boolean keepNonXzIndexFiles)
			throws IOException, RepositoryLocationURISyntaxException {

		URI location;
		try {
			location = RepositoryHelper.localRepoURIHelper(new URI(repository.getUrl()));
		} catch (Exception e) {
			throw new RepositoryLocationURISyntaxException(e);
		}
		if (outputLocation != null) {
			if (location.getScheme().equals("file") && Files.isSameFile(outputLocation, Path.of(location))) {
				outputLocation = null; // Signal in-place edit
			} else if (!Files.isDirectory(outputLocation)) {
				Files.createDirectories(outputLocation);
			} else {
				try (Stream<Path> files = Files.list(outputLocation);) {
					if (files.findAny().isPresent()) {
						throw new IllegalStateException("Output location is not empty: " + outputLocation);
					}
				}
			}
		}
		return new ModifiedRepositoryDescriptor(new MavenRepositoryLocation(repository.getId(), location),
				repositoryKind, outputLocation, compress, xzCompress, keepNonXzIndexFiles);
	}

	public record ModifiedRepositoryDescriptor(MavenRepositoryLocation repository, P2RepositoryKind repositoryKind,
			Path outputLocation, boolean compress, boolean xzCompress, boolean keepNonXzIndexFiles) {

		public boolean isArtifact() {
			return repositoryKind == null || repositoryKind == P2RepositoryKind.artifact;
		}

		public boolean isMetadata() {
			return repositoryKind == null || repositoryKind == P2RepositoryKind.metadata;
		}
	}

	@Inject
	private P2RepositoryManager manager;

	public void modifyRepositoryMetadata(ModifiedRepositoryDescriptor repository, String repositoryName,
			List<String> propertiesToRemove, Map<String, String> propertiesToAdd)
			throws ProvisionException, IOException {

		Consumer<IRepository<?>> modification = repo -> {
			if (repositoryName != null) {
				repo.setProperty(IRepository.PROP_NAME, repositoryName);
			}
			repo.setProperty(IRepository.PROP_COMPRESSED, Boolean.toString(repository.compress()));
			if (propertiesToRemove != null) {
				for (String key : propertiesToRemove) {
					repo.setProperty(key, null);
				}
			}
			if (propertiesToAdd != null) {
				propertiesToAdd.forEach((key, value) -> {
					repo.setProperty(key, value);
				});
			}
		};

		MavenRepositoryLocation location = repository.repository();
		Path outputLocation = repository.outputLocation();

		if (repository.isArtifact()) {
			modifyOutputRepository(manager.getArtifactRepository(location), outputLocation,
					manager::mirrorArtifactRepositoryData, r -> r.executeBatch(m -> modification.accept(r), null));
		}
		if (repository.isMetadata()) {
			modifyOutputRepository(manager.getMetadataRepository(location), outputLocation,
					manager::mirrorMetadataRepository, r -> r.executeBatch(m -> modification.accept(r), null));
		}
		xzCompress(repository, outputLocation);
	}

	@FunctionalInterface
	private interface ThrowingBiFunction<T, U, R, E extends Throwable> {
		R apply(T t, U u) throws E;
	}

	private <T, R extends IRepository<T>> void modifyOutputRepository(R repository, Path outputLocation,
			ThrowingBiFunction<R, Path, R, ProvisionException> copyAction, Consumer<R> modification)
			throws ProvisionException {
		if (outputLocation != null) {
			R localRepository = copyAction.apply(repository, outputLocation);
			modification.accept(localRepository);
		} else if (repository.isModifiable()) {
			modification.accept(repository);
		} else {
			throw new IllegalStateException(
					"Repository is not modifable and no output location is defined: " + repository.getLocation());
		}
	}

	private void xzCompress(ModifiedRepositoryDescriptor repository, Path outputLocation) throws IOException {
		if (repository.xzCompress()) {
			XZCompressor xzCompressor = new XZCompressor();
			xzCompressor.setPreserveOriginalFile(repository.keepNonXzIndexFiles());
			xzCompressor.setRepoFolder(outputLocation.toAbsolutePath().toString());
			xzCompressor.compressRepo();
		}
	}

	public void modifyCompositeRepository(ModifiedRepositoryDescriptor repository, String repositoryName,
			List<URI> childrenToAdd, List<URI> childrenToRemove, int childLimit)
			throws ProvisionException, IOException {
		if (childLimit > 0 && childrenToAdd.size() > childLimit) {
			throw new IllegalArgumentException("Cannot add more children than the specified limit of " + childLimit);
		}

		Consumer<CompositeRepositoryState> modification = repo -> {
			if (repositoryName != null) {
				repo.setName(repositoryName);
			}
			SequencedSet<URI> children = new LinkedHashSet<>(Arrays.asList(repo.getChildren()));
			for (URI child : childrenToRemove) {
				children.remove(child); // Child may or may not be in the list
			}
			for (URI child : childrenToAdd) {
				children.add(child);
			}
			if (childLimit > 0) {
				while (children.size() > childLimit) {
					children.removeFirst();
				}
			}
			repo.setChildren(children.toArray(URI[]::new));
		};

		if (repository.isArtifact()) {
			modifyOutputCompositeRepository(repository,
					(m, r) -> ((CompositeArtifactRepository) m.getArtifactRepository(r)).toState(), state -> {
						state.setType(CompositeArtifactRepository.REPOSITORY_TYPE);
						state.setVersion("1");
						state.getProperties().put(CompositeArtifactRepository.PROP_ATOMIC_LOADING, "true");
					}, modification, CompositeArtifactRepository.PI_REPOSITORY_TYPE,
					CompositeArtifactRepository.CONTENT_FILENAME);
		}
		if (repository.isMetadata()) {
			modifyOutputCompositeRepository(repository,
					(m, r) -> ((CompositeMetadataRepository) m.getMetadataRepository(r)).toState(), state -> {
						state.setType(CompositeMetadataRepository.REPOSITORY_TYPE);
						state.setVersion("1");
						state.getProperties().put(CompositeMetadataRepository.PROP_ATOMIC_LOADING, "true");
					}, modification, CompositeMetadataRepository.PI_REPOSITORY_TYPE,
					CompositeMetadataRepositoryFactory.CONTENT_FILENAME);
		}
		createP2Index(repository.outputLocation(), repository.isMetadata(), repository.isArtifact());
	}

	private <T, M extends IRepositoryManager<T>, R extends ICompositeRepository<T>> void modifyOutputCompositeRepository(
			ModifiedRepositoryDescriptor descriptor,
			ThrowingBiFunction<P2RepositoryManager, MavenRepositoryLocation, CompositeRepositoryState, ProvisionException> getState,
			Consumer<CompositeRepositoryState> initState, Consumer<CompositeRepositoryState> modification,
			String piRepositoryType, String filename) throws ProvisionException, IOException {

		MavenRepositoryLocation repository = descriptor.repository();
		CompositeRepositoryState state;
		try {
			state = getState.apply(manager, repository);
			state.setProperties(new LinkedHashMap<>(state.getProperties())); // ensure modifiable
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
				state = new CompositeRepositoryState();
				state.setProperties(new LinkedHashMap<>());
				state.setChildren(new URI[0]);
				initState.accept(state);
			} else {
				throw e;
			}
		}
		Path outputLocation = descriptor.outputLocation();
		if (outputLocation == null) {
			try {
				outputLocation = Path.of(repository.getURL());
			} catch (Exception e) {
				throw new IllegalStateException(
						"Repository is not modifable and no output location is defined: " + repository.getURL(), e);
			}
		}

		modification.accept(state);

		boolean compress = descriptor.compress();
		try (OutputStream output = createCompositeFileOutputStream(outputLocation, filename, compress);) {
			state.getProperties().put(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			state.getProperties().put(IRepository.PROP_COMPRESSED, Boolean.toString(compress));
			new CompositeRepositoryIO().write(state, output, piRepositoryType);
		}
	}

	private static OutputStream createCompositeFileOutputStream(Path directory, String filename, boolean compress)
			throws IOException {
		Path file = directory.resolve(filename + CompositeArtifactRepository.XML_EXTENSION);
		Path jarFile = directory.resolve(filename + CompositeArtifactRepository.JAR_EXTENSION);
		Files.createDirectories(directory);
		if (!compress) {
			Files.deleteIfExists(jarFile);
			return Files.newOutputStream(file);
		} else {
			Files.deleteIfExists(file);
			JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile));
			stream.putNextEntry(new JarEntry(file.getFileName().toString()));
			return stream;
		}
	}

	private void createP2Index(Path directory, boolean metadata, boolean artifacts) throws IOException {
		Properties p2Index = new Properties();
		p2Index.setProperty("version", "1"); //$NON-NLS-1$//$NON-NLS-2$
		if (metadata) {
			p2Index.setProperty("metadata.repository.factory.order", "compositeContent.xml,!"); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (artifacts) {
			p2Index.setProperty("artifact.repository.factory.order", "compositeArtifacts.xml,!"); //$NON-NLS-1$//$NON-NLS-2$
		}
		try (OutputStream output = Files.newOutputStream(directory.resolve("p2.index"))) {
			p2Index.store(output, null);
		}
	}

}
