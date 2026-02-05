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
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.version.Version;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.tycho.artifacts.ArtifactVersion;

/**
 * Base class for Maven repository based {@link ArtifactVersion} implementations
 * that share common artifact resolution and OSGi info lookup logic.
 */
abstract class AbstractMavenArtifactVersion implements ArtifactVersion {

	protected final MavenArtifactVersionProvider provider;
	protected final Artifact artifact;
	protected final List<RemoteRepository> repositories;
	private Path path;

	protected AbstractMavenArtifactVersion(MavenArtifactVersionProvider provider, Artifact artifact, Version version,
			List<RemoteRepository> repositories) {
		this.provider = provider;
		this.artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(),
				version.toString());
		this.repositories = repositories;
	}

	@Override
	public Path getArtifact() {
		if (path == null) {
			try {
				ArtifactRequest request = new ArtifactRequest(artifact, repositories, "");
				ArtifactResult result = provider.repoSystem
						.resolveArtifact(provider.session.getRepositorySession(), request);
				path = result.getArtifact().getFile().toPath();
			} catch (ArtifactResolutionException e) {
			}
		}
		return path;
	}

	@Override
	public String toString() {
		return getVersion() + " (maven artifact " + artifact + ")";
	}

	@Override
	public String getProvider() {
		ModuleRevisionBuilder info = provider.readOSGiInfo(getArtifact());
		if (info != null) {
			return info.getSymbolicName() + " " + info.getVersion();
		}
		return null;
	}
}
