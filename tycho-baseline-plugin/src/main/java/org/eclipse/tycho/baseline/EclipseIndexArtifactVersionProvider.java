/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.baseline;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;
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

	@Inject
	public EclipseIndexArtifactVersionProvider(TransportCacheConfig cacheConfig,
			P2RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
		p2Index = new P2IndexImpl(new File(cacheConfig.getCacheLocation(), "index"));
	}

	@Override
	public Stream<ArtifactVersion> getPackageVersions(IInstallableUnit unit, String packageName,
			VersionRange versionRange, MavenProject mavenProject) {
		Map<Repository, Set<Version>> map = p2Index.lookupCapabilities(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE,
				packageName);
		Set<Version> found = new HashSet<>();
		String id = unit.getId();
		return map.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream().filter(v -> v.isOSGiCompatible()).filter(v -> found.add(v))
						.map(version -> new EclipseIndexArtifactVersion(entry.getKey(), id, packageName, version)))
				.filter(eia -> versionRange.includes(eia.getVersion()))
				.sorted(Comparator.comparing(EclipseIndexArtifactVersion::getVersion).reversed())
				.map(ArtifactVersion.class::cast);
	}

	private class EclipseIndexArtifactVersion implements ArtifactVersion {

		private Version version;
		private org.apache.maven.model.Repository repository;
		private String packageName;
		private Path tempFile;
		private String unitId;

		public EclipseIndexArtifactVersion(Repository repository, String unitId, String packageName, Version version) {
			this.repository = new org.apache.maven.model.Repository();
			this.repository.setUrl(repository.getLocation().toString());
			this.unitId = unitId;
			this.packageName = packageName;
			this.version = version;
		}

		@Override
		public Path getArtifact() {
			if (tempFile == null) {
				try {
					IInstallableUnit unit = getUnit();
					if (unit != null) {
						tempFile = Files.createTempFile(unit.getId(), ".jar");
						tempFile.toFile().deleteOnExit();
						try (OutputStream stream = Files.newOutputStream(tempFile)) {
							repositoryManager.downloadArtifact(unit,
									repositoryManager.getArtifactRepository(repository), stream);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return tempFile;
		}

		private IInstallableUnit getUnit() {
			try {
				IMetadataRepository metadataRepository = repositoryManager.getMetadataRepository(repository);
				return ArtifactMatcher.findPackage(packageName,
						metadataRepository.query(QueryUtil.createIUQuery(unitId), null), version).orElse(null);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public org.osgi.framework.Version getVersion() {
			return org.osgi.framework.Version.parseVersion(version.getOriginal());
		}

		@Override
		public String toString() {
			return getVersion() + " (from repository " + repository + ")";
		}

		@Override
		public String getProvider() {
			IInstallableUnit unit = getUnit();
			if (unit != null) {
				return unit.getId() + " " + unit.getVersion();
			}
			return null;
		}

	}

}
