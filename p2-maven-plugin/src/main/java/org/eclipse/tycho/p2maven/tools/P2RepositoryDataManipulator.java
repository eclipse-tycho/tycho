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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Repository;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.XZCompressor;
import org.eclipse.equinox.p2.repository.IRepository;
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
		P2RepositoryKind kind = repository.repositoryKind();
		Path outputLocation = repository.outputLocation();

		if (kind == null || kind == P2RepositoryKind.artifact) {
			modifyOutputRepository(manager.getArtifactRepository(location), outputLocation,
					manager::mirrorArtifactRepositoryData, r -> r.executeBatch(m -> modification.accept(r), null));
		}
		if (kind == null || kind == P2RepositoryKind.metadata) {
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

}
