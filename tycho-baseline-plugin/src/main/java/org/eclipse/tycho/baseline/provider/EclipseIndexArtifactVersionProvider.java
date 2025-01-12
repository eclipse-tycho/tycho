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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.copyfrom.oomph.P2Index;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;
import org.eclipse.tycho.copyfrom.oomph.P2IndexImpl;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.p2maven.transport.TransportCacheConfig;
import org.osgi.framework.VersionRange;

/**
 * {@link ArtifactVersionProvider} using eclipse index
 */
@Named
public class EclipseIndexArtifactVersionProvider implements ArtifactVersionProvider {

	private P2Index p2Index;
	private P2RepositoryManager repositoryManager;
	private Logger logger;

	@Inject
	public EclipseIndexArtifactVersionProvider(TransportCacheConfig cacheConfig, P2RepositoryManager repositoryManager,
			Logger logger) {
		this.repositoryManager = repositoryManager;
		this.logger = logger;
		p2Index = new P2IndexImpl(new File(cacheConfig.getCacheLocation(), "index"));
	}

	@Override
	public Stream<ArtifactVersion> getPackageVersions(IInstallableUnit unit, String packageName,
			VersionRange versionRange, MavenProject mavenProject) {
		Map<Repository, Set<Version>> map = p2Index.lookupCapabilities(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE,
				packageName);
		Map<Version, List<Repository>> found = new HashMap<>();
		map.entrySet().forEach(entry -> {
			entry.getValue().stream().filter(v -> v.isOSGiCompatible()).forEach(v -> {
				found.computeIfAbsent(v, x -> new ArrayList<>()).add(entry.getKey());
			});
		});
		String id = unit.getId();
		return found.entrySet().stream().map(entry -> {
			return new EclipseIndexArtifactVersion(entry.getValue(), id, packageName, entry.getKey(), logger);
		}).filter(eia -> versionRange.includes(eia.getVersion()))
				.sorted(Comparator.comparing(EclipseIndexArtifactVersion::getVersion).reversed())
				.map(ArtifactVersion.class::cast);
	}

	private class EclipseIndexArtifactVersion implements ArtifactVersion {

		private Version version;
		private String packageName;
		private Path tempFile;
		private String unitId;
		private List<Repository> repositories;
		private org.osgi.framework.Version osgiVersion;
		private Optional<IInstallableUnit> unit;
		private Repository unitRepo;

		public EclipseIndexArtifactVersion(List<Repository> repositories, String unitId, String packageName,
				Version version, Logger logger) {
			osgiVersion = org.osgi.framework.Version.parseVersion(version.getOriginal());
			this.repositories = repositories;

			this.unitId = unitId;
			this.packageName = packageName;
			this.version = version;
		}

		@Override
		public Path getArtifact() {
			if (tempFile == null) {
				IInstallableUnit unit = getUnit().orElse(null);
				if (unit != null) {
					Path file;
					try {
						file = Files.createTempFile(unit.getId(), ".jar");
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
								repositoryManager.downloadArtifact(unit, repositoryManager.getArtifactRepository(r),
										stream);
								return tempFile = file;
							}
						} catch (Exception e) {
							logger.error("Fetch artifact for unit " + unit.getId() + " from " + repository.getLocation()
									+ " failed: " + e);
						}
					}
					file.toFile().delete();
				}
			}
			return tempFile;
		}

		private Optional<IInstallableUnit> getUnit() {
			if (unit == null) {
				for (Repository repository : repositories) {
					try {
						org.apache.maven.model.Repository r = new org.apache.maven.model.Repository();
						r.setUrl(repository.getLocation().toString());
						IMetadataRepository metadataRepository = repositoryManager.getMetadataRepository(r);
						Optional<IInstallableUnit> optional = ArtifactMatcher.findPackage(packageName,
								metadataRepository.query(QueryUtil.createIUQuery(unitId), null), version);
						if (optional.isPresent()) {
							this.unitRepo = repository;
							return unit = optional;
						} else {
							// if we have a package exported from many bundles it might be the case that the
							// actual unit we look for is not found, so we need to try on
							logger.debug(
									"Package " + packageName + " not found in metadata of " + repository.getLocation());
						}
					} catch (Exception e) {
						logger.error("Fetch metadata from " + repository.getLocation() + ":: " + e);
					}
				}
				unit = Optional.empty();
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
			return getUnit().map(unit -> unit.getId() + " " + unit.getVersion()).orElse(null);
		}

	}

}
