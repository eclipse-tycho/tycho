/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;

// TODO use interface with a subset of methods to ease stubbing?
public class ReactorProjectStub implements ReactorProject, ReactorProjectIdentities {
    private File basedir;

    private String groupId;

    private String artifactId;

    private String version;

    private String packagingType;

    private Set<Object> dependencyMetadata = new LinkedHashSet<Object>();

    private Set<Object> secondaryDependencyMetadata = new LinkedHashSet<Object>();

    public ReactorProjectStub(File basedir, String groupId, String artifactId, String version, String packagingType) {
        this.basedir = basedir;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packagingType = packagingType;
    }

    public File getBasedir() {
        return basedir;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packagingType;
    }

    public Set<?> getDependencyMetadata(boolean primary) {
        return primary ? dependencyMetadata : secondaryDependencyMetadata;
    }

    public void setDependencyMetadata(IDependencyMetadata dependencyMetadata) {
        this.dependencyMetadata = new LinkedHashSet<Object>(dependencyMetadata.getMetadata(true));
        this.secondaryDependencyMetadata = new LinkedHashSet<Object>(dependencyMetadata.getMetadata(false));
    }

    // TODO share with real implementation?
    public Set<?> getDependencyMetadata() {
        Set<?> primary = getDependencyMetadata(true);
        Set<?> secondary = getDependencyMetadata(false);

        if (primary == null) {
            return secondary;
        } else if (secondary == null) {
            return primary;
        }

        LinkedHashSet<Object> result = new LinkedHashSet<Object>(primary);
        result.addAll(secondary);
        return result;
    }

    public ReactorProjectIdentities getIdentities() {
        // TODO use something with correct equals implementation?
        return this;
    }

    public File getOutputDirectory() {
        return new File(getBasedir(), "target");
    }

    public BuildOutputDirectory getBuildDirectory() {
        return new BuildOutputDirectory(getOutputDirectory());
    }

    public File getTestOutputDirectory() {
        throw new UnsupportedOperationException();
    }

    public File getArtifact() {
        return null;
    }

    public File getArtifact(String artifactClassifier) {
        return null;
    }

    public Object getContextValue(String key) {
        return null;
    }

    public void setContextValue(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public void setDependencyMetadata(boolean primary, Set<?> installableUnits) {
        throw new UnsupportedOperationException();
    }

    public String getExpandedVersion() {
        return getVersion();
    }

    public void setExpandedVersion(String originalVersion, String qualifier) {
        throw new UnsupportedOperationException();
    }

    public String getId() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public boolean sameProject(Object otherProject) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
}
