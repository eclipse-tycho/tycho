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

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.osgi.framework.VersionRange;

/**
 * A {@link ArtifactVersionProvider} that checks maven repository for possible
 * candidates
 */
@Named
@SessionScoped
public class MavenArtifactVersionProvider implements ArtifactVersionProvider {

	MavenSession session;
	RepositorySystem repoSystem;
	private BundleReader bundleReader;

	@Inject
	public MavenArtifactVersionProvider(MavenSession session, RepositorySystem repoSystem, BundleReader bundleReader) {
		this.session = session;
		this.repoSystem = repoSystem;
		this.bundleReader = bundleReader;
	}

	@Override
	public Stream<ArtifactVersion> getPackageVersions(IInstallableUnit unit, String packageName,
			VersionRange versionRange, MavenProject mavenProject) {
		String groupId = unit.getProperty(TychoConstants.PROP_GROUP_ID);
		String artifactId = unit.getProperty(TychoConstants.PROP_ARTIFACT_ID);
		String classifier = unit.getProperty(TychoConstants.PROP_CLASSIFIER);
		if (groupId != null && artifactId != null && !"sources".equals(classifier)) {
			List<RemoteRepository> repositories = RepositoryUtils.toRepos(mavenProject.getRemoteArtifactRepositories());
			DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, "jar", "[0,)");
			// as we have no mean for a package version in maven we can only fetch all
			// versions an check if the match
			VersionRangeRequest rangeRequest = new VersionRangeRequest(artifact, repositories, "");
			try {
				VersionRangeResult range = repoSystem.resolveVersionRange(session.getRepositorySession(), rangeRequest);
				// now we sort from highest > lowest version
				List<Version> versions = range.getVersions().stream()
						.sorted(Comparator.<Version>naturalOrder().reversed()).toList();
				return versions.stream()
						.map(v -> new MavenPackageArtifactVersion(this, artifact, v, packageName, repositories))
						.filter(mav -> mav.getVersion() != null)
						// and drop all until we find a matching version
						.dropWhile(mav -> !versionRange.includes(mav.getVersion()))
						// and stop when we find the first non matching version
						.takeWhile(mav -> versionRange.includes(mav.getVersion()))
						// cast to make compiler happy
						.map(ArtifactVersion.class::cast);
			} catch (VersionRangeResolutionException e) {
				// can't provide any useful data then...
			}
		}
		return Stream.empty();
	}

	@Override
	public Stream<ArtifactVersion> getBundleVersions(IInstallableUnit unit, String bundleName,
			VersionRange versionRange, MavenProject mavenProject) {
		String groupId = unit.getProperty(TychoConstants.PROP_GROUP_ID);
		String artifactId = unit.getProperty(TychoConstants.PROP_ARTIFACT_ID);
		String classifier = unit.getProperty(TychoConstants.PROP_CLASSIFIER);
		if (groupId != null && artifactId != null && !"sources".equals(classifier)) {
			List<RemoteRepository> repositories = RepositoryUtils.toRepos(mavenProject.getRemoteArtifactRepositories());
			DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, "jar", "[0,)");
			VersionRangeRequest rangeRequest = new VersionRangeRequest(artifact, repositories, "");
			try {
				VersionRangeResult range = repoSystem.resolveVersionRange(session.getRepositorySession(), rangeRequest);
				// now we sort from highest > lowest version
				List<Version> versions = range.getVersions().stream()
						.sorted(Comparator.<Version>naturalOrder().reversed()).toList();
				return versions.stream()
						.map(v -> new MavenBundleArtifactVersion(this, artifact, v, bundleName, repositories))
						.filter(mav -> mav.getVersion() != null)
						// and drop all until we find a matching version
						.dropWhile(mav -> !versionRange.includes(mav.getVersion()))
						// and stop when we find the first non matching version
						.takeWhile(mav -> versionRange.includes(mav.getVersion()))
						// cast to make compiler happy
						.map(ArtifactVersion.class::cast);
			} catch (VersionRangeResolutionException e) {
				// can't provide any useful data then...
			}
		}
		return Stream.empty();
	}

	ModuleRevisionBuilder readOSGiInfo(Path path) {
		if (path != null) {
			try {
				OsgiManifest manifest = bundleReader.loadManifest(path.toFile());
				return OSGiManifestBuilderFactory.createBuilder(manifest.getHeaders());
			} catch (Exception e) {
				// On maven there might be all kind of badly formated manifests ... if we can't
				// parse it and create even a basic valid OSGi one than the artifacts is
				// unlikely usable in OSGI context anyways
			}
		}
		return null;
	}

}
