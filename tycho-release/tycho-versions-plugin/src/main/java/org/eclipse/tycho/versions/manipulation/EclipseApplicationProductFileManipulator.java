/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.MutablePomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-application")
public class EclipseApplicationProductFileManipulator extends ProductFileManipulator {

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (isEclipseApplication(project)) {
            applyChangeToProduct(project, getProductConfiguration(project), getProductFileName(project), change);
        }
    }

    public Collection<String> validateChange(ProjectMetadata project, VersionChange change) {
        if (isEclipseApplication(project)) {
            ProductConfiguration product = getProductConfiguration(project);
            if (isSameProject(project, change.getProject()) && change.getVersion().equals(product.getVersion())) {
                String error = Versions.validateOsgiVersion(change.getNewVersion(), getProductFile(project));
                return error != null ? Collections.singleton(error) : null;
            }
        }
        return null;
    }

    private ProductConfiguration getProductConfiguration(ProjectMetadata project) {
        ProductConfiguration product = project.getMetadata(ProductConfiguration.class);
        if (product == null) {
            File file = getProductFile(project);
            try {
                product = ProductConfiguration.read(file);
                project.putMetadata(product);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read product configuration file " + file, e);
            }
        }
        return product;
    }

    private File getProductFile(ProjectMetadata project) {
        return new File(project.getBasedir(), getProductFileName(project));
    }

    private String getProductFileName(ProjectMetadata project) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        return pom.getArtifactId() + ".product";
    }

    private boolean isEclipseApplication(ProjectMetadata project) {
        String packaging = project.getMetadata(MutablePomFile.class).getPackaging();
        return isEclipseApplication(packaging);
    }

    private boolean isEclipseApplication(String packaging) {
        return ArtifactKey.TYPE_ECLIPSE_APPLICATION.equals(packaging);
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        ProductConfiguration product = project.getMetadata(ProductConfiguration.class);
        if (product != null) {
            ProductConfiguration.write(product, getProductFile(project));
        }
    }
}
