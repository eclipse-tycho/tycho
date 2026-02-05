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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.copyfrom.oomph.P2Index;
import org.eclipse.tycho.copyfrom.oomph.P2Index.Repository;
import org.eclipse.tycho.copyfrom.oomph.P2IndexImpl;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.p2maven.transport.TransportCacheConfig;
import org.osgi.framework.VersionRange;

/**
 * {@link ArtifactVersionProvider} using eclipse index
 */
@Named
public class EclipseIndexArtifactVersionProvider implements ArtifactVersionProvider {

	private P2Index p2Index;
	P2RepositoryManager repositoryManager;
	Logger logger;

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
			return new EclipseIndexPackageArtifactVersion(this, entry.getValue(), id, packageName, entry.getKey(), logger);
		}).filter(eia -> versionRange.includes(eia.getVersion()))
				.sorted(Comparator.comparing(EclipseIndexPackageArtifactVersion::getVersion).reversed())
				.map(ArtifactVersion.class::cast);
	}

	@Override
	public Stream<ArtifactVersion> getBundleVersions(IInstallableUnit unit, String bundleName,
			VersionRange versionRange, MavenProject mavenProject) {
		Map<Repository, Set<Version>> map = p2Index.lookupCapabilities(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
				bundleName);
		Map<Version, List<Repository>> found = new HashMap<>();
		map.entrySet().forEach(entry -> {
			entry.getValue().stream().filter(v -> v.isOSGiCompatible()).forEach(v -> {
				found.computeIfAbsent(v, x -> new ArrayList<>()).add(entry.getKey());
			});
		});
		return found.entrySet().stream().map(entry -> {
			return new EclipseIndexBundleArtifactVersion(this, entry.getValue(), bundleName, entry.getKey(), logger);
		}).filter(eia -> versionRange.includes(eia.getVersion()))
				.sorted(Comparator.comparing(EclipseIndexBundleArtifactVersion::getVersion).reversed())
				.map(ArtifactVersion.class::cast);
	}

}
