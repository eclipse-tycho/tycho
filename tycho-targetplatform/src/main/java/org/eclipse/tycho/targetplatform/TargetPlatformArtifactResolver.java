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
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * This component resolves a given target artifact to a target platform file
 *
 */
public interface TargetPlatformArtifactResolver {

	public static final String TARGET_TYPE = "target";

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
	public File resolveTargetFile(String groupId, String artifactId, String version, String classifier,
			MavenSession session, List<ArtifactRepository> remoteRepositories) throws TargetResolveException;

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
	public Optional<File> getReactorTargetFile(String groupId, String artifactId, String version, String classifier,
			MavenSession session) throws TargetResolveException;

	public static File getMainTargetFile(MavenProject project) throws TargetResolveException {
		File[] targetFiles = TargetDefinitionFile.listTargetFiles(project.getBasedir());
		if (targetFiles == null || targetFiles.length == 0) {
			throw new TargetResolveException(
					"No target definition file(s) found in project '" + project.getName() + "'.");
		}
		if (targetFiles.length == 1) {
			return targetFiles[0];
		}
		for (File targetFile : targetFiles) {
			String baseName = FilenameUtils.getBaseName(targetFile.getName());
			if (baseName.equalsIgnoreCase(project.getArtifactId())) {
				return targetFile;
			}
		}
		throw new TargetResolveException("One target file must be named  '" + project.getArtifactId()
				+ TargetDefinitionFile.FILE_EXTENSION + "' when multiple targets are present");
	}

}
