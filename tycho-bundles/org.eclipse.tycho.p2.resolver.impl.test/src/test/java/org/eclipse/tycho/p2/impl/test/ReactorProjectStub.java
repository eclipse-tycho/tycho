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
package org.eclipse.tycho.p2.impl.test;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    private Map<String, Object> contextValues = new HashMap<>();

    private Set<?> dependencyMetadata = new LinkedHashSet<>();

    private Set<?> secondaryDependencyMetadata = new LinkedHashSet<>();

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

    @Override
    public Set<?> getDependencyMetadata(boolean primary) {
        return primary ? dependencyMetadata : secondaryDependencyMetadata;
    }

    public void setDependencyMetadata(IDependencyMetadata dependencyMetadata) {
        this.dependencyMetadata = new LinkedHashSet<>(dependencyMetadata.getDependencyMetadata(true));
        this.secondaryDependencyMetadata = new LinkedHashSet<>(dependencyMetadata.getDependencyMetadata(false));
    }

    @Override
    public void setDependencyMetadata(boolean primary, Set<?> installableUnits) {
        if (primary)
            this.dependencyMetadata = installableUnits;
        else
            this.secondaryDependencyMetadata = installableUnits;
    }

    // TODO share with real implementation?
    @Override
    public Set<?> getDependencyMetadata() {
        Set<?> primary = getDependencyMetadata(true);
        Set<?> secondary = getDependencyMetadata(false);

        if (primary == null) {
            return secondary;
        } else if (secondary == null) {
            return primary;
        }

        LinkedHashSet<Object> result = new LinkedHashSet<>(primary);
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
}
