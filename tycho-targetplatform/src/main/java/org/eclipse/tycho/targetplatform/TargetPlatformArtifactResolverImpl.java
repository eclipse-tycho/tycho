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
 *    Christoph Läubrich - initial API and implementation based on DefaultTargetPlatformConfigurationReader
 *******************************************************************************/
package org.eclipse.tycho.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * This component resolves a given target artifact to a target platform file
 *
 */
@Singleton
@Named
public class TargetPlatformArtifactResolverImpl implements TargetPlatformArtifactResolver {

	@Inject
	private RepositorySystem repositorySystem;

	/**
	 * Resolves the target file artifact with the given coordinates, session and
	 * remote repositories
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param classifier
	 * @param session
	 * @param remoteRepositories
	 * @return the target file for the specified artifact
	 * @throws TargetResolveException if resolving the target fails
	 */
	@Override
	public File resolveTargetFile(String groupId, String artifactId, String version, String classifier,
			MavenSession session, List<ArtifactRepository> remoteRepositories) throws TargetResolveException {
		// check if target is part of reactor-build
		Optional<File> reactorTargetFile = getReactorTargetFile(groupId, artifactId, version, classifier, session);
		if (reactorTargetFile.isPresent()) {
			return reactorTargetFile.get();
		}
		// resolve using maven
		ArtifactRequest request = new ArtifactRequest();
		DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, TARGET_TYPE, version);
		request.setArtifact(artifact);
		List<RemoteRepository> repos = new ArrayList<>(RepositoryUtils.toRepos(remoteRepositories));
		repos.add(RepositoryUtils.toRepo(session.getLocalRepository()));
		request.setRepositories(repos);
		try {
			ArtifactResult result = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
			File file = result.getArtifact().getFile();
			if (file != null && file.exists()) {
				return file;
			}
		} catch (ArtifactResolutionException e) {
			throw new TargetResolveException("Could not resolve target platform specification artifact " + artifact, e);
		}
		throw new TargetResolveException("Could not resolve target platform specification artifact " + artifact);
	}

	/**
	 * Lookup a given target artifact in the current reactor
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param classifier
	 * @param session
	 * @return an empty optional if no reactor project matches or an optional
	 *         describing the local file of this target artifact in the reactor
	 * @throws TargetResolveException
	 */
	@Override
	public Optional<File> getReactorTargetFile(String groupId, String artifactId, String version, String classifier,
			MavenSession session) throws TargetResolveException {
		if (session == null) {
			return Optional.empty();
		}
		List<MavenProject> projects = session.getProjects();
		if (projects == null) {
			return Optional.empty();
		}
		for (MavenProject project : projects) {
			if (groupId.equals(project.getGroupId()) && artifactId.equals(project.getArtifactId())
					&& version.equals(project.getVersion())) {
				if (classifier == null || classifier.isBlank()) {
					return Optional.of(TargetPlatformArtifactResolver.getMainTargetFile(project));
				} else {
					File target = new File(project.getBasedir(), classifier + TargetDefinitionFile.FILE_EXTENSION);
					if (TargetDefinitionFile.isTargetFile(target)) {
						return Optional.of(target);
					} else {
						throw new TargetResolveException("target definition file '" + target
								+ "' not found in project '" + project.getName() + "'.");
					}
				}
			}
		}
		return Optional.empty();
	}

}
