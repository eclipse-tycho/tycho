/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - actually lookup artifacts by P2 repository
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.repository.artifact.ArtifactDescriptorQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2maven.LoggerProgressMonitor;

public class P2RepositoryConnector implements RepositoryConnector {

	private final RemoteRepository repository;
	private RepositorySystemSession session;
	private RepositoryLayout repositoryLayout;
	private IArtifactRepository artifactRepository;
	private Logger log;

	public P2RepositoryConnector(RemoteRepository repository, IArtifactRepository artifactRepository,
			RepositorySystemSession session, RepositoryLayout repositoryLayout, Logger log) {
		this.repository = repository;
		this.artifactRepository = artifactRepository;
		this.session = session;
		this.repositoryLayout = repositoryLayout;
		this.log = log;
	}

	@Override
	public void get(Collection<? extends ArtifactDownload> artifactDownloads,
			Collection<? extends MetadataDownload> metadataDownloads) {
		if (artifactDownloads != null) {
			for (ArtifactDownload a : artifactDownloads) {
				Artifact artifact = a.getArtifact();
				String property = artifact.getProperty("type", null);
				if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(property)) {
					Iterator<IArtifactDescriptor> iterator = descriptorsOf(artifact).iterator();
					if (a.isExistenceCheck()) {
						if (iterator.hasNext()) {
							continue;
						}
					} else {
						File file = downloadArtifact(iterator);
						if (file != null) {
							a.setFile(file);
							continue;
						}
					}
				}
				a.setException(new ArtifactNotFoundException(artifact, repository));
			}
		}
		if (metadataDownloads != null) {
			for (MetadataDownload m : metadataDownloads) {
				m.setException(new MetadataNotFoundException(m.getMetadata(), repository));
			}
		}
	}

	private File downloadArtifact(Iterator<IArtifactDescriptor> iterator) {
		File basedir = session.getLocalRepository().getBasedir();
		while (iterator.hasNext()) {
			IArtifactDescriptor descriptor = iterator.next();
			IArtifactKey key = descriptor.getArtifactKey();
			URI location = repositoryLayout.getLocation(
					new DefaultArtifact(TychoConstants.P2_GROUPID_PREFIX + key.getClassifier(), key.getId(), "jar",
							key.getVersion().toString()),
					false);
			File file = new File(basedir, location.toString());
			if (file.isFile()) {
				return file;
			}
			file.getParentFile().mkdirs();
			//TODO respect the batch mode from the session!
			log.info("Downloading " + descriptor.getArtifactKey() + " to " + file.getAbsolutePath() + "...");
			try (FileOutputStream outputStream = new FileOutputStream(file)) {
				artifactRepository.getArtifact(descriptor, outputStream, new LoggerProgressMonitor(log));
				return file;
			} catch (IOException e) {
				log.error("Download failed!", e);
				file.delete();
			}
		}
		return null;
	}

	private Stream<IArtifactDescriptor> descriptorsOf(Artifact artifact) {
		Version version = Version.create(artifact.getVersion());
		IArtifactKey key = artifactRepository.createArtifactKey(PublisherHelper.OSGI_BUNDLE_CLASSIFIER,
				artifact.getArtifactId(), version);
		return Stream
				.of(new ArtifactDescriptorQuery(key),
						new ArtifactDescriptorQuery(artifact.getArtifactId(),
								new VersionRange(version, true, Version.MAX_VERSION, false), null))
				.flatMap(q -> artifactRepository.descriptorQueryable().query(q, new LoggerProgressMonitor(log)).toSet()
						.stream());
	}

	@Override
	public void put(Collection<? extends ArtifactUpload> artifactUploads,
			Collection<? extends MetadataUpload> metadataUploads) {
		//TODO actually we can support uploads to local repository locations!
		if (artifactUploads != null) {
			for (ArtifactUpload a : artifactUploads) {
				a.setException(new ArtifactNotFoundException(a.getArtifact(), repository));
			}
		}
		if (metadataUploads != null) {
			for (MetadataUpload m : metadataUploads) {
				m.setException(new MetadataNotFoundException(m.getMetadata(), repository));
			}
		}
	}

	@Override
	public void close() {
	}

}
