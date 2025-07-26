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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import org.eclipse.tycho.p2maven.repository.P2RepositoryKind;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;

@Named
@Singleton
public class P2RepositoryDataManipulator {

	public static ModifiedRepositoryDescriptor createDescriptor(Repository repositoryLocation,
			P2RepositoryKind repositoryKind, Path outputLocation, boolean compress, boolean xzCompress,
			boolean keepNonXzIndexFiles) throws IllegalArgumentException {

		if (outputLocation != null) {
			try {
				URI location = RepositoryHelper.localRepoURIHelper(new URI(repositoryLocation.getUrl()));
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
				repositoryLocation.setUrl(location.toString()); // reset (potentially) normalized URL
			} catch (IOException e) {
				throw new IllegalArgumentException("Not a writeable directory", e);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Not a valid location: " + repositoryLocation.getUrl(), e);
			}
		}
		return new ModifiedRepositoryDescriptor(repositoryLocation, repositoryKind, outputLocation, compress,
				xzCompress, keepNonXzIndexFiles);
	}

	public record ModifiedRepositoryDescriptor(Repository sourceRepository, P2RepositoryKind repositoryKind,
			Path outputLocation, boolean compress, boolean xzCompress, boolean keepNonXzIndexFiles) {
	}

	@Inject
	private P2RepositoryManager manager;

	public void modifyRepositoryMetadata(ModifiedRepositoryDescriptor repository, String repositoryName,
			List<String> propertiesToRemove, Map<String, String> propertiesToAdd)
			throws ProvisionException, URISyntaxException, IOException {

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

		Repository location = repository.sourceRepository();
		P2RepositoryKind kind = repository.repositoryKind();
		Path outputLocation = repository.outputLocation();

		if (kind.isArtifact()) {
			modifyOutputRepository(manager.getArtifactRepository(location), outputLocation,
					manager::mirrorArtifactRepositoryData, r -> r.executeBatch(m -> modification.accept(r), null));
		}
		if (kind.isMetadata()) {
			modifyOutputRepository(manager.getMetadataRepository(location), outputLocation,
					manager::mirrorMetadataRepository, r -> r.executeBatch(m -> modification.accept(r), null));
		}
		xzCompress(repository, outputLocation);
	}

	private <T, R extends IRepository<T>> void modifyOutputRepository(R repository, Path outputLocation,
			BiFunction<R, Path, R> copyAction, Consumer<R> modification) throws ProvisionException {
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
