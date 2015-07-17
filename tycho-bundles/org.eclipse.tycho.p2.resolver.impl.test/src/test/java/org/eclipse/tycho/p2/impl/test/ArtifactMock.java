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

import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;

public class ArtifactMock implements IArtifactFacade {
    private File location;

    private String groupId;

    private String artifactId;

    private String version;

    private String packagingType;

    private String classifier;

    private Set<Object> dependencyMetadata = new LinkedHashSet<>();

    private Set<Object> secondaryDependencyMetadata = new LinkedHashSet<>();

    public ArtifactMock(File location, String groupId, String artifactId, String version, String packagingType,
            String classifier) {
        this.location = location;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packagingType = packagingType;
        this.classifier = classifier;
    }

    public ArtifactMock(File location, String groupId, String artifactId, String version, String packagingType) {
        this(location, groupId, artifactId, version, packagingType, null);
    }

    public ArtifactMock(ReactorProjectStub project, String classifier) {
        this(project.getBasedir(), project.getGroupId(), project.getArtifactId(), project.getVersion(), project
                .getPackaging(), classifier);
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPackagingType() {
        return packagingType;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public Set<Object> getDependencyMetadata(boolean primary) {
        return primary ? dependencyMetadata : secondaryDependencyMetadata;
    }

    public void setDependencyMetadata(IDependencyMetadata dependencyMetadata) {
        this.dependencyMetadata = new LinkedHashSet<>(dependencyMetadata.getMetadata(true));
        this.secondaryDependencyMetadata = new LinkedHashSet<>(dependencyMetadata.getMetadata(false));
    }
}
