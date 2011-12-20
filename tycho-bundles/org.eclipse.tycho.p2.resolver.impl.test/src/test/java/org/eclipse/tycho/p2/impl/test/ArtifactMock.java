/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.util.Set;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;

public class ArtifactMock implements IArtifactFacade, IReactorArtifactFacade {
    private File location;

    private String groupId;

    private String artifactId;

    private String version;

    private String packagingType;

    private String classifier;

    private Set<Object> dependencyMetadata;

    private Set<Object> secondaryDependencyMetadata;

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

    public File getLocation() {
        return location;
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

    public String getPackagingType() {
        return packagingType;
    }

    public String getClassidier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public Set<Object> getDependencyMetadata(boolean primary) {
        return primary ? dependencyMetadata : secondaryDependencyMetadata;
    }

    public void setDependencyMetadata(IDependencyMetadata dependencyMetadata) {
        this.dependencyMetadata = dependencyMetadata.getMetadata(true);
        this.secondaryDependencyMetadata = dependencyMetadata.getMetadata(false);
    }
}
