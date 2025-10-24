/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.packaging.reverseresolve;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.TychoConstants;

/**
 * Uses data stored in the P2 metadata to map to maven artifacts
 *
 */
@Singleton
@Named("p2")
public class P2ArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	@Inject
	private RepositorySystem repositorySystem;

	@Inject
	private Logger log;

	@Override
	public Optional<Dependency> resolve(Dependency dependency, MavenProject project, MavenSession session) {
		if (dependency instanceof ArtifactDescriptor) {
			ArtifactDescriptor descriptor = (ArtifactDescriptor) dependency;
			return descriptor.getInstallableUnits().stream().map(iu -> {
				// Restore the original GAV in case the dependency is a rebundled jar
				String groupId = iu.getProperty(TychoConstants.PROP_WRAPPED_GROUP_ID);
				String artifactId = iu.getProperty(TychoConstants.PROP_WRAPPED_ARTIFACT_ID);
				String version = iu.getProperty(TychoConstants.PROP_WRAPPED_VERSION);
				String classifier = iu.getProperty(TychoConstants.PROP_WRAPPED_CLASSIFIER);
				// The classifier is optional and may be null
				if (groupId != null && artifactId != null && version != null) {
					Dependency result = new Dependency();
					result.setGroupId(groupId);
					result.setArtifactId(artifactId);
					result.setVersion(version);
					result.setType("jar");
					result.setClassifier(classifier);
					return result;
				}

				groupId = iu.getProperty(TychoConstants.PROP_GROUP_ID);
				artifactId = iu.getProperty(TychoConstants.PROP_ARTIFACT_ID);
				version = iu.getProperty(TychoConstants.PROP_VERSION);
				if (groupId != null && artifactId != null && version != null) {
					ArtifactTypeRegistry typeRegistry = session.getRepositorySession().getArtifactTypeRegistry();
					ArtifactType type = typeRegistry
							.get(Objects.requireNonNullElse(iu.getProperty(TychoConstants.PROP_TYPE), "jar"));
					Artifact artifact = new DefaultArtifact(groupId, artifactId,
							iu.getProperty(TychoConstants.PROP_CLASSIFIER), type != null ? type.getExtension() : null,
							version);
					try {
						ArtifactRequest request = new ArtifactRequest(artifact, project.getRemoteProjectRepositories(),
								null);
						Artifact resolved = repositorySystem.resolveArtifact(session.getRepositorySession(), request)
								.getArtifact();
						if (resolved != null) {
							Dependency result = new Dependency();
							result.setGroupId(resolved.getGroupId());
							result.setArtifactId(resolved.getArtifactId());
							result.setVersion(resolved.getVersion());
							result.setType(resolved.getExtension());
							result.setClassifier(resolved.getClassifier());
							return result;
						}
					} catch (Exception e) {
						log.debug("Cannot resolve from repository system because of " + e, e);
					}
				}
				return null;
			}).filter(Objects::nonNull).findFirst();
		}
		return Optional.empty();
	}

}
