/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProductConfigurations;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.MutablePomFile;

@Component(role = MetadataManipulator.class, hint = "eclipse-repository")
public class EclipseRepositoryProductFileManipulator extends ProductFileManipulator {

    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (!isEclipseRepository(project)) {
            return;
        }
        for (Map.Entry<File, ProductConfiguration> entry : getProductConfigurations(project).entrySet()) {
            applyChangeToProduct(project, entry.getValue(), entry.getKey().getName(), change);
        }
    }

    public Collection<String> validateChange(ProjectMetadata project, VersionChange change) {
        if (isEclipseRepository(project)) {
            ArrayList<String> errors = new ArrayList<String>();
            for (Map.Entry<File, ProductConfiguration> entry : getProductConfigurations(project).entrySet()) {
                if (isSameProject(project, change.getProject())
                        && change.getVersion().equals(entry.getValue().getVersion())) {
                    String error = Versions.validateOsgiVersion(change.getNewVersion(), entry.getKey());
                    if (error != null) {
                        errors.add(error);
                    }
                }
            }
            if (!errors.isEmpty()) {
                return errors;
            }
        }
        return null;
    }

    private boolean isEclipseRepository(ProjectMetadata project) {
        return ArtifactKey.TYPE_ECLIPSE_REPOSITORY.equals(project.getMetadata(MutablePomFile.class).getPackaging());
    }

    public void writeMetadata(ProjectMetadata project) throws IOException {
        ProductConfigurations products = project.getMetadata(ProductConfigurations.class);
        if (products != null) {
            for (Map.Entry<File, ProductConfiguration> entry : products.getProductConfigurations().entrySet()) {
                ProductConfiguration.write(entry.getValue(), entry.getKey());
            }
        }
    }

    private Map<File, ProductConfiguration> getProductConfigurations(ProjectMetadata project) {
        ProductConfigurations products = project.getMetadata(ProductConfigurations.class);
        if (products == null) {
            products = new ProductConfigurations();
            File[] productFiles = project.getBasedir().listFiles(new FileFilter() {

                public boolean accept(File file) {
                    return file.isFile() && file.getName().endsWith(".product");
                }
            });
            if (productFiles != null) {
                for (File productFile : productFiles) {
                    try {
                        products.addProductConfiguration(productFile, ProductConfiguration.read(productFile));
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Could not read product configuration file " + productFile,
                                e);
                    }
                }
            }
            project.putMetadata(products);
        }
        return products.getProductConfigurations();
    }

}
