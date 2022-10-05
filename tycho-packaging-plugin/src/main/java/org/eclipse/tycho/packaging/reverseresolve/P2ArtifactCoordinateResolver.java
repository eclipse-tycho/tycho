/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
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
@Component(role = ArtifactCoordinateResolver.class, hint = "p2")
public class P2ArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	@Requirement
	private RepositorySystem repositorySystem;

	@Requirement
	private Logger log;

	@Override
	public Optional<Dependency> resolve(Dependency dependency, MavenProject project, MavenSession session) {
		if (dependency instanceof ArtifactDescriptor) {
			ArtifactDescriptor descriptor = (ArtifactDescriptor) dependency;
			return descriptor.getInstallableUnits().stream().map(iu -> {
				String groupId = iu.getProperty(TychoConstants.PROP_GROUP_ID);
				String artifactId = iu.getProperty(TychoConstants.PROP_ARTIFACT_ID);
				String version = iu.getProperty(TychoConstants.PROP_VERSION);
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
						log.debug("Can't resolve from repository system because of " + e, e);
					}
				}
				return null;
			}).filter(Objects::nonNull).findFirst();
		}
		return Optional.empty();
	}

}
