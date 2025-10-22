/*******************************************************************************
 * Copyright (c) 2023, 2024 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.tycho.PackagingType;

/**
 * An implementation of a workspace reader similar to what maven does but for the Tycho packaging
 * types
 */
@SessionScoped
@Named(TychoReactorReader.HINT)
public class TychoReactorReader implements MavenWorkspaceReader {
    static final String HINT = "tycho-reactor";

    private final Map<String, MavenProject> projectsByGAV;
    private final WorkspaceRepository repository;

    @Inject
    public TychoReactorReader(MavenSession session) {
        this.projectsByGAV = session.getProjects().stream()
                .collect(toMap(s -> ArtifactUtils.key(s.getGroupId(), s.getArtifactId(), s.getVersion()), identity()));
        repository = new WorkspaceRepository(HINT, null);
    }

    @Override
    public WorkspaceRepository getRepository() {
        return repository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        return getTychoReactorProject(artifact).map(project -> {
            org.apache.maven.artifact.Artifact mainArtifact = project.getArtifact();
            if (mainArtifact != null && mainArtifact.getFile() != null && mainArtifact.getFile().exists()) {
                return mainArtifact.getFile();
            }
            return project.getBasedir();
        }).orElse(null);
    }

    public Optional<MavenProject> getTychoReactorProject(Artifact artifact) {
        if (isTychoReactorArtifact(artifact)) {
            String projectKey = ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion());
            MavenProject project = projectsByGAV.get(projectKey);
            if (project != null) {
                if (PackagingType.TYCHO_PACKAGING_TYPES.contains(project.getPackaging())) {
                    return Optional.of(project);
                }
            }
        }
        return Optional.empty();
    }

    public boolean isTychoReactorArtifact(Artifact artifact) {
        if (artifact.getClassifier() == null || artifact.getClassifier().isBlank()) {
            return PackagingType.TYCHO_PACKAGING_TYPES.contains(getPackagingType(artifact));
        }
        return false;
    }

    public String getPackagingType(Artifact artifact) {
        if (artifact != null) {
            return artifact.getProperty("type", "");
        }
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        return getTychoReactorProject(artifact).map(project -> List.of(artifact.getVersion())).orElse(List.of());
    }

    @Override
    public Model findModel(Artifact artifact) {
        MavenProject project = projectsByGAV
                .get(ArtifactUtils.key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        return project == null ? null : project.getModel();
    }

}
