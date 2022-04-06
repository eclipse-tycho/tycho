/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.ArtifactCollection;

public class DefaultArtifactDescriptor implements ArtifactDescriptor {

    private final ArtifactKey key;

    private Function<ArtifactDescriptor, File> locationSupplier;
    private File location;

    private final ReactorProject project;

    private final String classifier;

    private final Set<Object> installableUnits;

    public DefaultArtifactDescriptor(ArtifactKey key, File location, ReactorProject project, String classifier,
            Set<Object> installableUnits) {
        this.key = key;
        this.location = ArtifactCollection.normalizeLocation(location);
        this.project = project;
        this.classifier = classifier;
        this.installableUnits = installableUnits;
    }

    public DefaultArtifactDescriptor(ArtifactKey key, Function<ArtifactDescriptor, File> location,
            ReactorProject project, String classifier, Set<Object> installableUnits) {
        this.key = key;
        this.locationSupplier = location;
        this.project = project;
        this.classifier = classifier;
        this.installableUnits = installableUnits;
    }

    @Override
    public ArtifactKey getKey() {
        return key;
    }

    @Override
    public File getLocation(boolean fetch) {
        if (location == null && locationSupplier != null && fetch) {
            File file = locationSupplier.apply(this);
            if (file != null) {
                location = ArtifactCollection.normalizeLocation(file);
            }
        }
        return location;
    }

    @Override
    public ReactorProject getMavenProject() {
        return project;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public Set<Object> getInstallableUnits() {
        return installableUnits;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key.toString()).append(": ");
        if (project != null) {
            sb.append(project.toString());
        } else {
            sb.append(location);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, locationSupplier, locationSupplier == null ? location : null, classifier, project,
                installableUnits);
    }

    @Override
    public boolean equals(Object obj) {
        // explicitly disallow comparison with subclasses
        if (obj == null || obj.getClass() != DefaultArtifactDescriptor.class) {
            return false;
        }

        DefaultArtifactDescriptor other = (DefaultArtifactDescriptor) obj;

        return Objects.equals(key, other.key)
                && (Objects.equals(location, other.location)
                        || Objects.equals(locationSupplier, other.locationSupplier))
                && Objects.equals(project, other.project) && Objects.equals(classifier, other.classifier)
                && Objects.equals(installableUnits, other.installableUnits);
    }

}
