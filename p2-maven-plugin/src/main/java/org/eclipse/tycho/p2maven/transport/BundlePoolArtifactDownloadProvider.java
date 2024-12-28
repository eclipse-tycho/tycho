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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.helper.MavenPropertyHelper;
import org.eclipse.tycho.transport.ArtifactDownloadProvider;

/**
 * Provides artifacts already available from the users bundle pools
 */
@Named
public class BundlePoolArtifactDownloadProvider implements ArtifactDownloadProvider {

	private SimpleArtifactRepositoryFactory artifactRepositoryFactory;
	private Map<Path, IArtifactRepository> repositoryMap = new ConcurrentHashMap<>();
	private TransportCacheConfig cacheConfig;
	private Logger logger;
	private boolean useSharedPools;
	private boolean useWorkspacePools;
	private int priority;

	@Inject
	public BundlePoolArtifactDownloadProvider(SimpleArtifactRepositoryFactory artifactRepositoryFactory,
			TransportCacheConfig cacheConfig, Logger logger, MavenPropertyHelper propertyHelper) {
		this.artifactRepositoryFactory = artifactRepositoryFactory;
		this.cacheConfig = cacheConfig;
		this.logger = logger;
		useSharedPools = propertyHelper.getGlobalBooleanProperty("tycho.p2.transport.bundlepools.shared", true);
		useWorkspacePools = propertyHelper.getGlobalBooleanProperty("tycho.p2.transport.bundlepools.workspace", true);
		priority = propertyHelper.getGlobalIntProperty("tycho.p2.transport.bundlepools.priority", 100);

	}

	@Override
	public IStatus downloadArtifact(URI source, OutputStream target, IArtifactDescriptor originalDescriptor) {
		return pools().parallel().flatMap(path -> {
			IArtifactRepository repository = getRepository(path);
			if (repository instanceof IFileArtifactRepository filerepository) {
				IArtifactKey artifactKey = originalDescriptor.getArtifactKey();
				File artifactFile = filerepository.getArtifactFile(artifactKey);
				if (artifactFile != null) {
					return Arrays.stream(repository.getArtifactDescriptors(artifactKey)).map(
							descriptor -> new RepositoryCandidate(filerepository, descriptor, artifactFile.toPath()));
				}
			}
			return Stream.empty();
		}).filter(cand -> isMatch(cand, originalDescriptor)).findAny().map(candidate -> {
			IArtifactRepository repository = candidate.repository();
			if (cacheConfig.isInteractive()) {
				logger.info("Reading from " + repository.getName() + ": " + candidate.artifactFile());
			}
			return copyToTarget(target, candidate.artifactFile());
		}).orElse(Status.CANCEL_STATUS);
	}

	/**
	 * Test if two descriptors have at least one matching hashsum in which case we
	 * assume they are describing the same artifact and not only have the same
	 * version/id, this should not happen usually, but as we use global pools here
	 * it is better to be safe than sorry.
	 * 
	 * @param candidate          the candidate we want to use
	 * @param originalDescriptor the original descriptor queried
	 * @return <code>true</code> if at least one hashsum matches in both descriptors
	 */
	private boolean isMatch(RepositoryCandidate candidate, IArtifactDescriptor originalDescriptor) {
		Path artifactFile = candidate.artifactFile();
		if (Files.isRegularFile(artifactFile)) {
			// we can only use files as we need to process them as if downloaded from a real
			// server...
			IArtifactDescriptor repositoryDescriptor = candidate.descriptor();
			// now see if we can perform a fast check by comparing original hashsums
			for (Entry<String, String> entry : originalDescriptor.getProperties().entrySet()) {
				String key = entry.getKey();
				if (key.startsWith(TychoConstants.PROP_DOWNLOAD_CHECKSUM_PREFIX)) {
					String property = repositoryDescriptor.getProperty(key);
					if (property != null) {
						String value = entry.getValue();
						return value.equals(property);
					}
				}
			}
			if (fileSizeMatch(repositoryDescriptor, originalDescriptor)) {
				// if we are here, then it means no download checksums where present for
				// comparison and we need to generate one ourself
				for (Entry<String, String> entry : originalDescriptor.getProperties().entrySet()) {
					String key = entry.getKey();
					if (key.startsWith(TychoConstants.PROP_DOWNLOAD_CHECKSUM_PREFIX)) {
						try {
							String algorithm = key.substring(TychoConstants.PROP_DOWNLOAD_CHECKSUM_PREFIX.length())
									.toUpperCase();
							MessageDigest md = MessageDigest.getInstance(algorithm);
							try (DigestOutputStream outputStream = new DigestOutputStream(
									OutputStream.nullOutputStream(), md);
									InputStream inputStream = Files.newInputStream(artifactFile)) {
								inputStream.transferTo(outputStream);
							}
							return ChecksumHelper.toHexString(md.digest()).equals(entry.getValue());
						} catch (Exception e) {
							// can't check...
						}
					}
				}
			}
		}
		return false;
	}

	private static IStatus copyToTarget(OutputStream target, Path path) {
		try {
			Files.copy(path, target);
		} catch (IOException e) {
			return Status.error("Can't copy file to target", e);
		}
		DownloadStatus status = new DownloadStatus(IStatus.OK, "org.eclipse.tycho", "File " + path, null);
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

	private boolean fileSizeMatch(IArtifactDescriptor repositoryDescriptor, IArtifactDescriptor originalDescriptor) {
		String originalSize = originalDescriptor.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE);
		if (originalSize != null) {
			String property = repositoryDescriptor.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE);
			if (property != null) {
				return originalSize.equals(property);
			}
		}
		// assume true for further processing
		return true;
	}

	private Stream<Path> pools() {
		if (useSharedPools) {
			if (useWorkspacePools) {
				List<Path> sharedBundlePools = RepositoryHelper.getSharedBundlePools();
				List<Path> workspaceBundlePools = RepositoryHelper.getWorkspaceBundlePools();
				return Stream.concat(sharedBundlePools.stream(), workspaceBundlePools.stream()).distinct();
			} else {
				return RepositoryHelper.getSharedBundlePools().stream();
			}
		} else if (useWorkspacePools) {
			return RepositoryHelper.getWorkspaceBundlePools().stream();
		} else {
			return Stream.empty();
		}
	}

	private IArtifactRepository getRepository(Path path) {
		return repositoryMap.computeIfAbsent(path, p -> {
			try {
				return artifactRepositoryFactory.load(path.toUri(), 0, null);
			} catch (ProvisionException e) {
				return null;
			}
		});
	}

	private static record RepositoryCandidate(IFileArtifactRepository repository, IArtifactDescriptor descriptor,
			Path artifactFile) {

	}

	@Override
	public int getPriority() {
		return priority;
	}

}
