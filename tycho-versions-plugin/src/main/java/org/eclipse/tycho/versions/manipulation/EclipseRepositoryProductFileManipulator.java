/*******************************************************************************
 * Copyright (c) 2013, 2017 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Sebastien Arod - introduce VersionChangesDescriptor
 *     Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/

package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.eclipse.tycho.versions.engine.ProductConfigurations;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;
import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.utils.ProductFileFilter;

@Named("eclipse-repository-products")
@Singleton
public class EclipseRepositoryProductFileManipulator extends ProductFileManipulator {

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (!isEclipseRepository(project)) {
            return;
        }
        for (PomVersionChange change : versionChangeContext.getVersionChanges()) {
            for (Map.Entry<File, ProductConfiguration> entry : getProductConfigurations(project).entrySet()) {
                applyChangeToProduct(project, entry.getValue(), entry.getKey().getName(), change);
            }
        }
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isEclipseRepository(project)) {
            for (PomVersionChange change : versionChangeContext.getVersionChanges()) {
                ArrayList<String> errors = new ArrayList<>();
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
        }
        return null;
    }

    private boolean isEclipseRepository(ProjectMetadata project) {
        return PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(project.getMetadata(PomFile.class).getPackaging());
    }

    @Override
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
            File[] productFiles = project.getBasedir().listFiles(new ProductFileFilter());
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
