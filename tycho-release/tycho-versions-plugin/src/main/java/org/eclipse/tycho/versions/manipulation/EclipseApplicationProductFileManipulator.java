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
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.MutablePomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-application")
public class EclipseApplicationProductFileManipulator extends ProductFileManipulator {

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (isEclipseApplication(project)) {
            applyChangeToProduct(project, getProductFile(project), getProductFileName(project), change);
        }
    }

    private ProductConfiguration getProductFile(ProjectMetadata project) {
        ProductConfiguration product = project.getMetadata(ProductConfiguration.class);
        if (product == null) {
            String productFileName = getProductFileName(project);
            File file = new File(project.getBasedir(), productFileName);
            try {
                product = ProductConfiguration.read(file);
                project.putMetadata(product);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read product configuration file " + file, e);
            }
        }
        return product;
    }

    protected String getProductFileName(ProjectMetadata project) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        String productFileName = pom.getArtifactId() + ".product";
        return productFileName;
    }

    private boolean isEclipseApplication(ProjectMetadata project) {
        String packaging = project.getMetadata(MutablePomFile.class).getPackaging();
        return isEclipseApplication(packaging);
    }

    private boolean isEclipseApplication(String packaging) {
        return ArtifactKey.TYPE_ECLIPSE_APPLICATION.equals(packaging);
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        ProductConfiguration product = project.getMetadata(ProductConfiguration.class);
        if (product != null) {
            ProductConfiguration.write(product, new File(project.getBasedir(), pom.getArtifactId() + ".product"));
        }
    }
}
