/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.ArtifactCollection;

public class DefaultArtifactDescriptor implements ArtifactDescriptor {

    private final ArtifactKey key;

    private Function<ArtifactDescriptor, File> locationSupplier;
    private File location;

    private ReactorProject project;

    private final String classifier;

    private final Collection<IInstallableUnit> installableUnits;

    public DefaultArtifactDescriptor(ArtifactKey key, File location, ReactorProject project, String classifier,
            Collection<IInstallableUnit> installableUnits) {
        this.key = key;
        this.location = ArtifactCollection.normalizeLocation(location);
        this.project = project;
        this.classifier = classifier;
        this.installableUnits = installableUnits;
    }

    public DefaultArtifactDescriptor(ArtifactKey key, Function<ArtifactDescriptor, File> location,
            ReactorProject project, String classifier, Collection<IInstallableUnit> installableUnits) {
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
        if (project != null) {
            File basedir = project.getBasedir();
            if (basedir != null) {
                return basedir;
            }
        }
        if (fetch && locationSupplier != null && (location == null || !location.exists())) {
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
    public Collection<IInstallableUnit> getInstallableUnits() {
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

    public void resolve(File newLocation) {
        location = newLocation;
    }

    public void setMavenProject(ReactorProject mavenProject) {
        project = mavenProject;
    }

}
