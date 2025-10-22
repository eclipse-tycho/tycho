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
package org.eclipse.tycho.core.maven;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.LegacySupport;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;

/**
 * This component allows to resolve target artifacts from the reactor. Maven itself only supports a
 * limited way of resolving types in the reactor and do not know how to handle a "target type"
 * project.
 */
@Named("TargetPlatformWorkspaceReader")
@Singleton
public class TargetPlatformWorkspaceReader implements WorkspaceReader {

    @Inject
    private TargetPlatformArtifactResolver platformArtifactResolver;
    @Inject
    private LegacySupport legacySupport;
    private WorkspaceRepository repository;

    public TargetPlatformWorkspaceReader() {
        repository = new WorkspaceRepository("tycho-target-platform", null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        if (TargetPlatformArtifactResolver.TARGET_TYPE.equals(artifact.getExtension())) {
            try {
                Optional<File> targetFile = platformArtifactResolver.getReactorTargetFile(artifact.getGroupId(),
                        artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier(),
                        legacySupport.getSession());
                return targetFile.orElse(null);
            } catch (TargetResolveException e) {
                // something went wrong, so we can't find the requested artifact here...
            }
        }
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return List.of();
    }

}
