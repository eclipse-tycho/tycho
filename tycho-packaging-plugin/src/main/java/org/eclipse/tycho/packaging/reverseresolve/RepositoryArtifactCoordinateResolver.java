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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.shared.DependencyResolutionException;

/**
 * This resolves against the maven (local) repository if the jar carry a
 * suitable pom.properties file...
 */
@Component(role = ArtifactCoordinateResolver.class, hint = "local")
public class RepositoryArtifactCoordinateResolver implements ArtifactCoordinateResolver {

	@Requirement
	private RepositorySystem repositorySystem;

	@Requirement
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
					Dependency dependency = new Dependency();
					dependency.setGroupId(groupId);
					dependency.setArtifactId(artifactId);
					dependency.setType(type);
					dependency.setVersion(version);
					Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
					ArtifactResolutionRequest request = new ArtifactResolutionRequest();
					request.setArtifact(artifact);
					request.setOffline(session.isOffline());
					request.setLocalRepository(session.getLocalRepository());
					repositorySystem.injectMirror(request.getRemoteRepositories(), session.getSettings().getMirrors());
					repositorySystem.injectProxy(request.getRemoteRepositories(), session.getSettings().getProxies());
					repositorySystem.injectAuthentication(request.getRemoteRepositories(),
							session.getSettings().getServers());
					ArtifactResolutionResult resolveResult = repositorySystem.resolve(request);
					if (resolveResult.hasExceptions()) {
						DependencyResolutionException exception = new DependencyResolutionException(
								"resolving " + artifact + " failed!", resolveResult.getExceptions());
						log.debug("Resolve " + artifact + " failed because of " + exception, exception);
						return Optional.empty();

					}
					return resolveResult.getArtifacts().stream().filter(a -> a.getFile() != null).filter(a -> {
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
						result.setType(a.getType());
						return result;
					}).findFirst();
				}

			} catch (Exception e) {
				log.debug("Can't process " + path + " because of " + e, e);
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
