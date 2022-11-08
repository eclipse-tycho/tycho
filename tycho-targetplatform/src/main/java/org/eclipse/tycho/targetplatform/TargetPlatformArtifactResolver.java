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

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * This component resolves a given target artifact to a target platform file
 *
 */
@Component(role = TargetPlatformArtifactResolver.class)
public class TargetPlatformArtifactResolver {

	private static final String TARGET_TYPE = "target";

    @Requirement
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
    public File resolveTargetFile(String groupId, String artifactId, String version, String classifier,
			MavenSession session, List<ArtifactRepository> remoteRepositories) throws TargetResolveException {
        //check if target is part of reactor-build
        for (MavenProject project : session.getProjects()) {
            if (groupId.equals(project.getGroupId()) && artifactId.equals(project.getArtifactId())
                    && version.equals(project.getVersion())) {
                if (classifier == null) {
                    File[] targetFiles = TargetDefinitionFile
                            .listTargetFiles(project.getBasedir());
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
                } else {
                    File target = new File(project.getBasedir(),
                            classifier + TargetDefinitionFile.FILE_EXTENSION);
                    if (TargetDefinitionFile.isTargetFile(target)) {
                        return target;
                    } else {
						throw new TargetResolveException("target definition file '" + target
                                + "' not found in project '" + project.getName() + "'.");
                    }
                }
            }
        }
        // resolve using maven
		Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, TARGET_TYPE,
				classifier);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(remoteRepositories);
        repositorySystem.resolve(request);

        if (artifact.isResolved()) {
            return artifact.getFile();
        }
		throw new TargetResolveException("Could not resolve target platform specification artifact " + artifact);
    }

}
