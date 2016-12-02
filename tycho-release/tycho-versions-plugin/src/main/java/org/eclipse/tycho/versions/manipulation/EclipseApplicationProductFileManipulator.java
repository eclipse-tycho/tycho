/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - introduce VersionChangesDescriptor
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.PomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-application")
public class EclipseApplicationProductFileManipulator extends ProductFileManipulator {

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseApplication(project)) {
            for (VersionChange change : versionChangeContext.getVersionChanges()) {
                applyChangeToProduct(project, getProductConfiguration(project), getProductFileName(project), change);
            }
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseApplication(project)) {
            for (VersionChange change : versionChangeContext.getVersionChanges()) {
                ProductConfiguration product = getProductConfiguration(project);
                if (isSameProject(project, change.getProject()) && change.getVersion().equals(product.getVersion())) {
                    String error = Versions.validateOsgiVersion(change.getNewVersion(), getProductFile(project));
                    return error != null ? Collections.singleton(error) : null;
                }
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
        PomFile pom = project.getMetadata(PomFile.class);
        return pom.getArtifactId() + ".product";
    }

    private boolean isEclipseApplication(ProjectMetadata project) {
        String packaging = project.getMetadata(PomFile.class).getPackaging();
        return isEclipseApplication(packaging);
    }

    private boolean isEclipseApplication(String packaging) {
        return PackagingType.TYPE_ECLIPSE_APPLICATION.equals(packaging);
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        ProductConfiguration product = project.getMetadata(ProductConfiguration.class);
        if (product != null) {
            ProductConfiguration.write(product, getProductFile(project));
        }
    }
}
