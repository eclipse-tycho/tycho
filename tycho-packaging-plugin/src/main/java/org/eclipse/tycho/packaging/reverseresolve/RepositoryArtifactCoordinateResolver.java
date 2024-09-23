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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * This resolves against the maven (local) repository if the jar carry a
 * suitable pom.properties file...
 */
@Singleton
@Named("local")
public class RepositoryArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	@Inject
	private RepositorySystem repositorySystem;

	@Inject
	private Logger log;

	@Override
	public Optional<Dependency> resolve(Dependency dep, MavenProject project, MavenSession session) {

		return ArtifactCoordinateResolver.getPath(dep).flatMap(path -> {
			try {
				Properties properties = getArtifactProperties(path);
				if (properties == null) {
					return Optional.empty();
				}
				String artifactId = properties.getProperty("artifactId");
				String groupId = properties.getProperty("groupId");
				String version = properties.getProperty("version");
				if (artifactId != null && groupId != null && version != null) {
					String type = FilenameUtils.getExtension(path.getFileName().toString());
					Artifact artifact = new DefaultArtifact(groupId, artifactId, type, version);
					ArtifactRequest artifactRequest = new ArtifactRequest();
					artifactRequest.setArtifact(artifact);
					artifactRequest.setRepositories(RepositoryUtils.toRepos(project.getRemoteArtifactRepositories()));
					ArtifactResult artifactResult = repositorySystem.resolveArtifact(session.getRepositorySession(),
							artifactRequest);
					if (artifactResult.isResolved()) {

					} else {
						log.debug("Resolving " + artifact + " failed because of: " + artifactResult.getExceptions());
						return Optional.empty();
					}
					return Optional.ofNullable(artifactResult.getArtifact()).filter(a -> a.getFile() != null)
							.filter(a -> {
								try {
									return FileUtils.contentEquals(a.getFile(), path.toFile());
								} catch (IOException e) {
									return false;
								}
							}).map(a -> {
								Dependency result = new Dependency();
								result.setGroupId(a.getGroupId());
								result.setArtifactId(a.getArtifactId());
								result.setVersion(a.getVersion());
								result.setType(a.getExtension());
								return result;
							});
				}

			} catch (Exception e) {
				log.debug("Cannot process " + path + " because of " + e, e);
			}

			return Optional.empty();
		});

	}

	private static Properties getArtifactProperties(Path path) throws IOException {
		if (Files.isRegularFile(path) && Files.size(path) > 0) {
			try (JarFile jarFile = new JarFile(path.toFile())) {
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {
						try (InputStream stream = jarFile.getInputStream(jarEntry)) {
							Properties properties = new Properties();
							properties.load(stream);
							properties.setProperty("file-type",
									FilenameUtils.getExtension(path.getFileName().toString()));
							return properties;
						}
					}
				}
			}
		}
		return null;
	}

}
