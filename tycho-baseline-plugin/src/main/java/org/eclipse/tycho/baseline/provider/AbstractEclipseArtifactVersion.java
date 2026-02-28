/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.baseline.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;

/**
 * Base class for Eclipse p2 index based {@link ArtifactVersion} implementations
 * that share common artifact download and unit lookup logic.
 */
abstract class AbstractEclipseArtifactVersion implements ArtifactVersion {

	private final EclipseIndexArtifactVersionProvider provider;
	protected final Version version;
	protected final List<Repository> repositories;
	private final org.osgi.framework.Version osgiVersion;
	private Path tempFile;
	protected Optional<IInstallableUnit> unit;
	protected Repository unitRepo;

	protected AbstractEclipseArtifactVersion(EclipseIndexArtifactVersionProvider provider,
			List<Repository> repositories, Version version) {
		this.provider = provider;
		this.repositories = repositories;
		this.version = version;
		this.osgiVersion = org.osgi.framework.Version.parseVersion(version.getOriginal());
	}

	protected EclipseIndexArtifactVersionProvider getVersionProvider() {
		return provider;
	}

	@Override
	public Path getArtifact() {
		if (tempFile == null) {
			IInstallableUnit iu = getUnit().orElse(null);
			if (iu != null) {
				Path file;
				try {
					file = Files.createTempFile(iu.getId(), ".jar");
				} catch (IOException e) {
					return null;
				}
				file.toFile().deleteOnExit();
				List<Repository> list = new ArrayList<>(repositories);
				if (unitRepo != null) {
					list.remove(unitRepo);
					list.add(0, unitRepo);
				}
				for (Repository repository : list) {
					try {
						org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
						r.setUrl(repository.getLocation().toString());
						try (OutputStream stream = Files.newOutputStream(file)) {
							provider.repositoryManager.downloadArtifact(iu,
									provider.repositoryManager.getArtifactRepository(r), stream);
							return tempFile = file;
						}
					} catch (Exception e) {
						provider.logger.error("Fetch artifact for unit " + iu.getId() + " from "
								+ repository.getLocation() + " failed: " + e);
					}
				}
				file.toFile().delete();
			}
		}
		return tempFile;
	}

	/**
	 * Resolves the {@link IInstallableUnit} for this artifact version from the
	 * available repositories.
	 *
	 * @return an Optional containing the unit if found
	 */
	protected abstract Optional<IInstallableUnit> findUnit();

	private Optional<IInstallableUnit> getUnit() {
		if (unit == null) {
			unit = findUnit();
		}
		return Objects.requireNonNullElse(unit, Optional.empty());
	}

	@Override
	public org.osgi.framework.Version getVersion() {
		return osgiVersion;
	}

	@Override
	public String toString() {
		if (unit != null && unit.isPresent()) {
			return getVersion() + " (" + unit.get() + ")";
		}
		return getVersion().toString();
	}

	@Override
	public String getProvider() {
		return getUnit().map(u -> u.getId() + " " + u.getVersion()).orElse(null);
	}
}
