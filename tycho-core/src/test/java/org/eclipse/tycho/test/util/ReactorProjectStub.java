/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;

// TODO use interface with a subset of methods to ease stubbing?
public class ReactorProjectStub extends ReactorProjectIdentities implements ReactorProject {

    private File basedir;

    private String groupId;

    private String artifactId;

    private String version;

    private String packagingType;

    private Map<String, Object> contextValues = new ConcurrentHashMap<>();

    private Set<IInstallableUnit> dependencyMetadata = new LinkedHashSet<>();

    private Set<IInstallableUnit> secondaryDependencyMetadata = new LinkedHashSet<>();

    private Set<IInstallableUnit> initialMetadata;

    public ReactorProjectStub(File basedir, String groupId, String artifactId, String version, String packagingType) {
        this.basedir = basedir;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packagingType = packagingType;
    }

    public ReactorProjectStub(File basedir, String artifactId) {
        this(basedir, "testgroup", artifactId, "0.0.20-SNAPSHOT", null);
    }

    public ReactorProjectStub(String artifactId) {
        this(null, artifactId);
    }

    private static <T> T unlessNull(T value) {
        if (value == null)
            throw new UnsupportedOperationException();
        else
            return value;
    }

    @Override
    public File getBasedir() {
        return unlessNull(basedir);
    }

    @Override
    public String getGroupId() {
        return unlessNull(groupId);
    }

    @Override
    public String getArtifactId() {
        return unlessNull(artifactId);
    }

    @Override
    public String getVersion() {
        return unlessNull(version);
    }

    @Override
    public String getPackaging() {
        return unlessNull(packagingType);
    }

    public void setPackagingType(String packagingType) {
        this.packagingType = packagingType;
    }

    @Override
    public Set<IInstallableUnit> getDependencyMetadata(DependencyMetadataType type) {
        return switch (type) {
        case SEED -> dependencyMetadata;
        case RESOLVE -> secondaryDependencyMetadata;
        case INITIAL -> initialMetadata;
        default -> Collections.emptySet();
        };
    }

    public void setDependencyMetadata(IDependencyMetadata dependencyMetadata) {
        this.dependencyMetadata = new LinkedHashSet<>(
                dependencyMetadata.getDependencyMetadata(DependencyMetadataType.SEED));
        this.secondaryDependencyMetadata = new LinkedHashSet<>(
                dependencyMetadata.getDependencyMetadata(DependencyMetadataType.RESOLVE));
        LinkedHashSet<IInstallableUnit> initial = new LinkedHashSet<>();
        initial.addAll(this.dependencyMetadata);
        initial.addAll(this.secondaryDependencyMetadata);
        this.initialMetadata = initial;
    }

    @Override
    public void setDependencyMetadata(DependencyMetadataType type, Collection<IInstallableUnit> units) {
        switch (type) {
        case SEED:
            this.dependencyMetadata = new LinkedHashSet<>(units);
            break;
        case RESOLVE:
            this.secondaryDependencyMetadata = new LinkedHashSet<>(units);
            break;
        case INITIAL:
            this.initialMetadata = new LinkedHashSet<>(units);
            break;
        }
    }

    // TODO share with real implementation?
    @Override
    public Set<IInstallableUnit> getDependencyMetadata() {
        Set<IInstallableUnit> primary = getDependencyMetadata(DependencyMetadataType.SEED);
        Set<IInstallableUnit> secondary = getDependencyMetadata(DependencyMetadataType.RESOLVE);

        if (primary == null) {
            return secondary;
        } else if (secondary == null) {
            return primary;
        }

        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<>(primary);
        result.addAll(secondary);
        return result;
    }

    @Override
    public ReactorProjectIdentities getIdentities() {
        // TODO use something with correct equals implementation?
        return this;
    }

    @Override
    public BuildDirectory getBuildDirectory() {
        return new BuildOutputDirectory(new File(getBasedir(), "target"));
    }

    @Override
    public File getArtifact() {
        return null;
    }

    @Override
    public File getArtifact(String artifactClassifier) {
        return null;
    }

    @Override
    public Object getContextValue(String key) {
        return contextValues.get(key);
    }

    @Override
    public void setContextValue(String key, Object value) {
        contextValues.put(key, value);
    }

    @Override
    public String getBuildQualifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getExpandedVersion() {
        return getVersion();
    }

    @Override
    public String getId() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sameProject(Object otherProject) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T adapt(Class<T> target) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T computeContextValue(String key, Supplier<T> initalValueSupplier) {
        return (T) contextValues.computeIfAbsent(key, x -> initalValueSupplier.get());
    }

}
