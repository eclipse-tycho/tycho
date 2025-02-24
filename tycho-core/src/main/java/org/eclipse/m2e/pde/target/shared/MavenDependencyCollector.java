/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Collector to collect (and filter) all transitive dependencies of a maven target location
 */
public class MavenDependencyCollector {

    private static final String TYPE_POM = "pom";

    private static final Set<String> VALID_EXTENSIONS = Set.of("jar", TYPE_POM);

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repositorySession;
    private final List<RemoteRepository> repositories;
    private final Collection<String> dependencyScopes;

    private DependencyDepth dependencyDepth;

    public MavenDependencyCollector(RepositorySystem repoSystem, RepositorySystemSession repositorySession,
            List<RemoteRepository> defaultRepositories, Collection<AdditionalRepository> additionalRepositories,
            DependencyDepth depth, Collection<String> dependencyScopes) {
        this.repoSystem = repoSystem;
        this.repositorySession = repositorySession;
        if (additionalRepositories == null || additionalRepositories.isEmpty()) {
            this.repositories = repoSystem.newResolutionRepositories(repositorySession, defaultRepositories);
        } else {
            List<RemoteRepository> combined = Stream
                    .concat(defaultRepositories.stream(), additionalRepositories.stream().map(additional -> {
                        return new RemoteRepository.Builder(additional.id(), "default", additional.url())
                                .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                                .setSnapshotPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                                .build();
                    })).distinct().toList();
            this.repositories = repoSystem.newResolutionRepositories(repositorySession, combined);
        }
        this.dependencyDepth = depth;
        this.dependencyScopes = dependencyScopes;
    }

    public List<RemoteRepository> getEffectiveRepositories() {
        return repositories;
    }

    public DependencyResult collect(MavenRootDependency root) throws RepositoryException {
        if (!VALID_EXTENSIONS.contains(root.getType())) {
            throw new RepositoryException(
                    "Invalid root dependency: " + root + " allowed types are " + VALID_EXTENSIONS);
        }
        DependencyDepth depth = getEffectiveDepth(root, dependencyDepth);
        List<RepositoryArtifact> artifacts = new ArrayList<>();
        List<DependencyNode> nodes = new ArrayList<>();
        ArtifactDescriptor rootDescriptor = readArtifactDescriptor(new Dependency(
                new DefaultArtifact(root.groupId(), root.artifactId(), root.classifier(), root.type(), root.version()),
                null), null, artifacts, nodes);
        if (depth == DependencyDepth.NONE) {
            return new DependencyResult(depth, artifacts, rootDescriptor.node(), nodes);
        }
        if (depth == DependencyDepth.DIRECT) {
            for (Dependency dependency : rootDescriptor.dependencies()) {
                readArtifactDescriptor(dependency, rootDescriptor.node(), artifacts, nodes);
            }
            return new DependencyResult(depth, artifacts, rootDescriptor.node(), nodes);
        }
        // Add all dependencies with BFS method
        Set<String> collected = new HashSet<>();
        collected.add(getId(rootDescriptor.node().getDependency()));
        Queue<ArtifactDescriptor> queue = new LinkedList<>();
        queue.add(rootDescriptor);
        while (!queue.isEmpty()) {
            ArtifactDescriptor current = queue.poll();
            for (Dependency dependency : current.dependencies()) {
                if (isValidDependency(dependency) && collected.add(getId(dependency))) {
                    ArtifactDescriptor dependencyDescriptor = readArtifactDescriptor(dependency, current.node(),
                            artifacts, nodes);
                    if (dependencyDescriptor != null) {
                        queue.add(dependencyDescriptor);
                    }
                }
            }
        }
        return new DependencyResult(depth, artifacts, rootDescriptor.node(), nodes);
    }

    /**
     * This method reads the artifact descriptor and resolves the artifact.
     * 
     * @param artifact
     *            the artifact to read its descriptor
     * @param artifacts
     * @param nodes
     * @return the resolved artifact and the list of (managed) dependencies
     * @throws RepositoryException
     */
    private ArtifactDescriptor readArtifactDescriptor(Dependency dependency, DependencyNode parent,
            Collection<RepositoryArtifact> artifacts, List<DependencyNode> nodes) throws RepositoryException {
        if (isValidScope(dependency) && isValidDependency(dependency)) {
            ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
            descriptorRequest.setArtifact(dependency.getArtifact());
            descriptorRequest.setRepositories(repositories);
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(repositorySession, descriptorRequest);
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(result.getArtifact());
            artifactRequest.setRepositories(repositories);
            ArtifactResult artifactResult = repoSystem.resolveArtifact(repositorySession, artifactRequest);
            Artifact resolved = artifactResult.getArtifact();
            artifacts.add(new RepositoryArtifact(resolved, artifactResult.getRepository()));
            DefaultDependencyNode dependencyNode = new DefaultDependencyNode(
                    new Dependency(resolved, dependency.getScope()));
            nodes.add(dependencyNode);
            if (parent != null) {
                parent.getChildren().add(dependencyNode);
            }
            return new ArtifactDescriptor(dependencyNode, result.getDependencies(), result.getManagedDependencies());
        }
        return null;
    }

    private boolean isValidDependency(Dependency dependency) {
        if (dependency.isOptional()) {
            // optional in maven means do not include in transitive dependency chains see
            // https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html
            return false;
        }
        return VALID_EXTENSIONS.contains(dependency.getArtifact().getExtension());
    }

    private boolean isValidScope(Dependency dependency) {
        String scope = dependency.getScope();
        if (scope == null || scope.isEmpty()) {
            return true;

        }
        return dependencyScopes.contains(scope);
    }

    public static DependencyDepth getEffectiveDepth(MavenRootDependency root, DependencyDepth dependencyDepth) {
        DependencyDepth depth;
        if (isClassified(root)) {
            // a classified artifact can not have any dependencies and will actually include
            // the ones from the main artifact.
            // if the user really wants this it is possible to include the pom typed
            // artifact or the main artifact in the list
            depth = DependencyDepth.NONE;
        } else if (dependencyDepth == DependencyDepth.NONE && TYPE_POM.equalsIgnoreCase(root.type())) {
            depth = DependencyDepth.DIRECT;
        } else {
            depth = dependencyDepth;
        }
        return depth;
    }

    private static String getId(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        // This does not include the version so we always ever only collect one version
        // of an (transitive) artifact
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getClassifier();
    }

    private static boolean isClassified(MavenRootDependency root) {
        String classifier = root.classifier();
        return classifier != null && !classifier.isBlank();
    }

}
