/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - add getName() / combine directories
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TychoProperties;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;

public class DefaultReactorProject implements ReactorProject {

    /**
     * Conventional key used to store ReactorProject in MavenProject.context
     */
    private static final String CTX_REACTOR_PROJECT = "tycho.reactor-project";

    /**
     * Conventional key used to store dependency metadata in MavenProject.context
     */
    private static final String CTX_DEPENDENCY_METADATA_PREFIX = "tycho.dependency-metadata-";

    public final MavenProject project;

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public DefaultReactorProject(MavenProject project) {
        if (project == null) {
            throw new NullPointerException();
        }

        this.project = project;
    }

    public static ReactorProject adapt(MavenProject project) {
        if (project == null) {
            return null;
        }

        ReactorProject reactorProject = (ReactorProject) project.getContextValue(CTX_REACTOR_PROJECT);
        if (reactorProject == null) {
            reactorProject = new DefaultReactorProject(project);
            project.setContextValue(CTX_REACTOR_PROJECT, reactorProject);
        }
        return reactorProject;
    }

    public static List<ReactorProject> adapt(MavenSession session) {
        ArrayList<ReactorProject> result = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            result.add(adapt(project));
        }
        return result;
    }

    @Override
    public boolean sameProject(Object otherProject) {
        return project.equals(otherProject);
    }

    @Override
    public File getBasedir() {
        return project.getBasedir();
    }

    @Override
    public String getPackaging() {
        return project.getPackaging();
    }

    @Override
    public String getGroupId() {
        return project.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return project.getArtifactId();
    }

    @Override
    public String getVersion() {
        return project.getVersion();
    }

    @Override
    public ReactorProjectIdentities getIdentities() {
        return new MavenReactorProjectIdentities(project);
    }

    @Override
    public BuildOutputDirectory getBuildDirectory() {
        return new BuildOutputDirectory(new File(project.getBuild().getDirectory()),
                new File(project.getBuild().getOutputDirectory()),
                new File(project.getBuild().getTestOutputDirectory()));
    }

    @Override
    public File getArtifact() {
        Artifact artifact = project.getArtifact();
        return artifact != null ? artifact.getFile() : null;
    }

    @Override
    public File getArtifact(String artifactClassifier) {
        Artifact artifact = null;
        if (artifactClassifier == null) {
            artifact = project.getArtifact();
        } else {
            for (Artifact attached : project.getAttachedArtifacts()) {
                if (artifactClassifier.equals(attached.getClassifier())) {
                    artifact = attached;
                    break;
                }
            }
        }
        return artifact != null ? artifact.getFile() : null;
    }

    @Override
    public Object getContextValue(String key) {
        Object value = context.get(key);
        return (value != null) ? value : project.getContextValue(key);
    }

    @Override
    public void setContextValue(String key, Object value) {
        Objects.requireNonNull(key, "key can't be null");
        if (value == null) {
            context.remove(key);
        } else {
            context.put(key, value);
        }
    }

    @Override
    public void setDependencyMetadata(DependencyMetadataType type, Collection<?> units) {
        setContextValue(getDependencyMetadataKey(type), units);
    }

    @Override
    public Set<?> getDependencyMetadata() {
        LinkedHashSet<Object> result = new LinkedHashSet<>(getDependencyMetadata(DependencyMetadataType.SEED));
        result.addAll(getDependencyMetadata(DependencyMetadataType.RESOLVE));
        return result;
    }

    @Override
    public Set<?> getDependencyMetadata(DependencyMetadataType type) {
        return Objects.requireNonNullElse((Set<?>) getContextValue(getDependencyMetadataKey(type)),
                Collections.emptySet());
    }

    private static String getDependencyMetadataKey(DependencyMetadataType type) {
        return CTX_DEPENDENCY_METADATA_PREFIX + type.name().toLowerCase();
    }

    @Override
    public String getBuildQualifier() {
        String version = (String) project.getProperties().get(TychoProperties.BUILD_QUALIFIER);
        if (version != null) {
            return version;
        }

        throw new IllegalStateException("Project " + getId() + " does not have a build qualifier");
    }

    @Override
    public String getExpandedVersion() {
        String version = (String) project.getProperties().get(TychoProperties.QUALIFIED_VERSION);
        if (version != null) {
            return version;
        }

        throw new IllegalStateException("Project " + getId() + " does not have an expanded version");
    }

    @Override
    public String getId() {
        return project.getId();
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DefaultReactorProject)) {
            return false;
        }

        DefaultReactorProject other = (DefaultReactorProject) obj;

        return project.equals(other.project);
    }

    @Override
    public String toString() {
        return project.toString();
    }

    @Override
    public String getName() {
        return project.getName();
    }
}
