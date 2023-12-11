/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.function.Supplier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.TychoProperties;
import org.eclipse.tycho.osgi.adapters.MavenReactorProjectIdentities;

public class DefaultReactorProject implements ReactorProject {

    /**
     * Conventional key used to store ReactorProject in MavenProject.context
     */
    private static final String CTX_REACTOR_PROJECT = "tycho.reactor-project."
            + System.identityHashCode(ReactorProject.class);

    /**
     * Conventional key used to store ReactorProject in MavenProject.context
     */
    private static final String CTX_MAVEN_SESSION = "tycho.reactor-project."
            + System.identityHashCode(MavenSession.class);

    /**
     * Conventional key used to store dependency metadata in MavenProject.context
     */
    private static final String CTX_DEPENDENCY_METADATA_PREFIX = "tycho.dependency-metadata-";

    final MavenProject project;

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    private File basedir;

    public DefaultReactorProject(MavenProject project) {
        if (project == null) {
            throw new NullPointerException();
        }
        this.project = project;
        ReactorProject reactorProject = getCachedValue(project);
        if (reactorProject != null) {
            this.basedir = reactorProject.getBasedir();
        } else {
            //we store the basedir here, just in case it gets modified e.g. by plugins setting a different pom file
            this.basedir = project.getBasedir();
        }
    }

    public static ReactorProject adapt(MavenProject project, MavenSession mavenSession) {
        ReactorProject reactorProject = adapt(project);
        if (mavenSession != null) {
            reactorProject.setContextValue(CTX_MAVEN_SESSION, mavenSession);
        }
        return reactorProject;
    }

    public static ReactorProject adapt(MavenProject project) {
        if (project == null) {
            return null;
        }
        synchronized (project) {
            ReactorProject reactorProject = getCachedValue(project);
            if (reactorProject == null) {
                reactorProject = new DefaultReactorProject(project);
                project.setContextValue(CTX_REACTOR_PROJECT, reactorProject);
            }
            return reactorProject;
        }

    }

    protected static ReactorProject getCachedValue(MavenProject project) {
        return project.getContextValue(CTX_REACTOR_PROJECT) instanceof ReactorProject reactorProject //
                ? reactorProject
                : null;
    }

    public static List<ReactorProject> adapt(MavenSession session) {
        if (session == null) {
            return List.of();
        }
        ArrayList<ReactorProject> result = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            ReactorProject reactorProject = adapt(project, session);
            reactorProject.computeContextValue(CTX_MAVEN_SESSION, () -> session);
            result.add(reactorProject);
        }
        return result;
    }

    @Override
    public boolean sameProject(Object otherProject) {
        return project.equals(otherProject);
    }

    @Override
    public File getBasedir() {
        if (basedir != null) {
            return basedir;
        }
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
        if (value instanceof LazyValue<?> lazy) {
            return lazy.get();
        }
        return (value != null) ? value : project.getContextValue(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T computeContextValue(String key, Supplier<T> initalValueSupplier) {
        Object value = context.computeIfAbsent(key, nil -> new LazyValue<>(initalValueSupplier));
        if (value instanceof LazyValue<?> lazy) {
            return (T) lazy.get();
        }
        return (T) value;
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
    public void setDependencyMetadata(DependencyMetadataType type, Collection<IInstallableUnit> units) {
        setContextValue(getDependencyMetadataKey(type), units);
    }

    @Override
    public Set<IInstallableUnit> getDependencyMetadata() {
        Set<IInstallableUnit> result = new LinkedHashSet<>(getDependencyMetadata(DependencyMetadataType.SEED));
        result.addAll(getDependencyMetadata(DependencyMetadataType.RESOLVE));
        return result;
    }

    @Override
    public Set<IInstallableUnit> getDependencyMetadata(DependencyMetadataType type) {
        @SuppressWarnings("unchecked")
        Set<IInstallableUnit> contextValue = (Set<IInstallableUnit>) getContextValue(getDependencyMetadataKey(type));
        return Objects.requireNonNullElse(contextValue, Collections.emptySet());
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
        return this == obj || //
                (obj instanceof DefaultReactorProject other && project.equals(other.project));
    }

    @Override
    public String toString() {
        return project.toString();
    }

    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public <T> T adapt(Class<T> target) {
        if (target == MavenSession.class) {
            return target.cast(getContextValue(CTX_MAVEN_SESSION));
        }
        if (target == MavenProject.class) {
            return target.cast(project);
        }
        return null;
    }

    private static final class LazyValue<T> implements Supplier<T> {

        private Supplier<T> initalValueSupplier;
        private T value;

        LazyValue(Supplier<T> initalValueSupplier) {
            this.initalValueSupplier = initalValueSupplier;
        }

        @Override
        public synchronized T get() {
            if (value == null) {
                value = initalValueSupplier.get();
            }
            return value;
        }

    }

}
