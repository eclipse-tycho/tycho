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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.eclipse.tycho.transport.ArtifactDownloadProvider;

/**
 * An artifact provider that query global configured maven repositories for a
 * given artifact
 */
@Named
@SessionScoped
public class MavenArtifactDownloadProvider implements ArtifactDownloadProvider {

	private MavenSession session;

	private RepositorySystem repoSystem;

	private boolean useMavenMirror;

	private int priority;

	@Inject
	public MavenArtifactDownloadProvider(MavenSession session, RepositorySystem repoSystem,
			MavenPropertyHelper propertyHelper) {
		this.session = session;
		this.repoSystem = repoSystem;
		useMavenMirror = propertyHelper.getGlobalBooleanProperty("tycho.p2.transport.mavenmirror.enabled", true);
		priority = propertyHelper.getGlobalIntProperty("tycho.p2.transport.mavenmirror.priority", 500);
	}

	@Override
	public IStatus downloadArtifact(URI source, OutputStream target, IArtifactDescriptor descriptor) {
		if (!useMavenMirror) {
			return Status.CANCEL_STATUS;
		}
		String groupId = descriptor.getProperty(TychoConstants.PROP_GROUP_ID);
		if (groupId == null) {
			return Status.CANCEL_STATUS;
		}
		String artifactId = descriptor.getProperty(TychoConstants.PROP_ARTIFACT_ID);
		if (artifactId == null) {
			return Status.CANCEL_STATUS;
		}
		String version = descriptor.getProperty(TychoConstants.PROP_VERSION);
		if (version == null) {
			return Status.CANCEL_STATUS;
		}
		if (version.endsWith("-SNAPSHOT")) {
			// sadly a lot of "bad" metadata is around that claims to be a SNAPSHOT version
			// but isn't Because of this we do not try to download SNAPSHOTS from maven
			// directly
			return Status.CANCEL_STATUS;
		}
		String classifer = descriptor.getProperty(TychoConstants.PROP_CLASSIFIER);
		if ("sources".equals(classifer)) {
			// sources require special treatment to be recognized as "source-bundles" unless
			// we have fixed this in PDE it is not very useful to download them from maven
			// as they almost always will mismatch in size
			return Status.CANCEL_STATUS;
		}
		String repository = descriptor.getProperty(TychoConstants.PROP_REPOSITORY);
		if (repository == null || repository.isBlank()) {
			// not fetched from a repository but probably only a local file
			return Status.CANCEL_STATUS;
		}
		// At best we would filter this list by the given repository id, but the ID
		// could be something like "eclipse.maven.central.mirror" instead of "central",
		// we need to find a way to get the "original" id of the server then it could be
		// a good alternative to only query those instead of all ..
		List<RemoteRepository> repositories = RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories());
		RepositorySystemSession repositorySession = session.getRepositorySession();
		ArtifactTypeRegistry stereotypes = repositorySession.getArtifactTypeRegistry();
		DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifer,
				stereotypes.get(Objects.requireNonNullElse(descriptor.getProperty(TychoConstants.PROP_TYPE), "jar"))
						.getExtension(),
				version);
		try {
			VersionRangeRequest rangeRequest = new VersionRangeRequest(new DefaultArtifact(artifact.getGroupId(),
					artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), "[0,)"), repositories,
					"");
			VersionRangeResult range = repoSystem.resolveVersionRange(repositorySession, rangeRequest);
			List<Version> versions = range.getVersions();
			if (versions.isEmpty()
					|| versions.stream().map(v -> v.toString()).noneMatch(s -> s.equals(artifact.getVersion()))) {
				return Status.CANCEL_STATUS;
			}
		} catch (VersionRangeResolutionException e) {
			return Status.CANCEL_STATUS;
		}
		Map<String, String> checksums = getChecksums(descriptor);
		// first check if we can obtain a matching checksum already from the server,
		// this has the advantage that we don't need to download the full artifact if
		// the checksum already has a mismatch
		boolean checksumMatch = false;
		for (Entry<String, String> entry : checksums.entrySet()) {
			ArtifactRequest artifactRequest = new ArtifactRequest(
					new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
							toMavenChecksumKey(entry.getKey(), artifact.getExtension()), artifact.getVersion()),
					repositories, null);
			try {
				Path path = resolveArtifact(repositorySession, artifactRequest).orElse(null);
				if (path != null) {
					String content = Files.readString(path);
					if (entry.getValue().equals(content)) {
						checksumMatch = true;
						break;
					}
					// checksum mismatch no need to further bother maven for this file
					return Status.CANCEL_STATUS;
				}
			} catch (Exception e) {
				// can't check this way...
			}
		}
		// now we have some good certainty this P2 artifact is actually sourced from
		// maven so lets fetch it ...
		ArtifactRequest artifactRequest = new ArtifactRequest(artifact, repositories, null);
		Path file = resolveArtifact(repositorySession, artifactRequest).orElse(null);
		if (file != null) {
			if (checksumMatch) {
				return copyToTarget(target, file, artifact, descriptor);
			}
			// we don't have had a previous match so lets calculate the checksum if the
			// filesize matches
			if (matchFileSize(file, descriptor)) {
				for (Entry<String, String> entry : checksums.entrySet()) {
					if (checksumMatch(file, entry)) {
						return copyToTarget(target, file, artifact, descriptor);
					}
				}
			}
		}
		return Status.CANCEL_STATUS;
	}

	private Optional<Path> resolveArtifact(RepositorySystemSession repositorySession, ArtifactRequest artifactRequest) {
		try {
			ArtifactResult result = repoSystem.resolveArtifact(repositorySession, artifactRequest);
			return Optional.ofNullable(result.getArtifact()).filter(a -> a.getFile() != null && a.getFile().isFile())
					.map(a -> a.getFile()).map(f -> f.toPath());
		} catch (ArtifactResolutionException e) {
			return Optional.empty();
		}
	}

	private static boolean checksumMatch(Path file, Entry<String, String> entry) {
		String key = entry.getKey();
		try {
			MessageDigest md = MessageDigest.getInstance(key.toUpperCase());
			try (InputStream inputStream = Files.newInputStream(file);
					DigestOutputStream outputStream = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
				inputStream.transferTo(outputStream);
			}
			return ChecksumHelper.toHexString(md.digest()).equals(entry.getValue());
		} catch (Exception e) {
		}
		return false;
	}

	private static IStatus copyToTarget(OutputStream target, Path path, Artifact resolved,
			IArtifactDescriptor descriptor) {
		try {
			Files.copy(path, target);
		} catch (IOException e) {
			return Status.error("Can't copy file to target", e);
		}
		DownloadStatus status = new DownloadStatus(IStatus.OK, "org.eclipse.tycho",
				"Download of " + descriptor.getArtifactKey() + " as " + resolved, null);
		try {
			status.setFileSize(Files.size(path));
		} catch (IOException e) {
		}
		try {
			status.setLastModified(Files.getLastModifiedTime(path).toMillis());
		} catch (IOException e) {
		}
		return status;
	}

	private static boolean matchFileSize(Path file, IArtifactDescriptor descriptor) {
		String sizeProperty = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE);
		if (sizeProperty != null) {
			try {
				return Long.parseLong(sizeProperty) == Files.size(file);
			} catch (NumberFormatException e) {
			} catch (IOException e) {
			}
		}
		// assume true to further process
		return true;
	}

	private static String toMavenChecksumKey(String key, String extension) {
		return extension + "." + key.replace("-", "");
	}

	private static Map<String, String> getChecksums(IArtifactDescriptor descriptor) {
		Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER.reversed());
		Map<String, String> properties = descriptor.getProperties();
		for (Entry<String, String> entry : properties.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith(TychoConstants.PROP_DOWNLOAD_CHECKSUM_PREFIX)) {
				map.put(key.substring(TychoConstants.PROP_DOWNLOAD_CHECKSUM_PREFIX.length()), entry.getValue());
			}
		}
		return map;
	}

	@Override
	public int getPriority() {
		return priority;
	}

}
