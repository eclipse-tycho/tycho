/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.ProductConfiguration;

/**
 * An eclipse repository project produces a p2 repository where a set of products are published.
 */
@Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_REPOSITORY)
public class EclipseRepositoryProject extends AbstractArtifactBasedProject {

    /**
     * The published repository is always under the id of the maven project: this published
     * repository can contain multiple products.
     */
    @Override
    public ArtifactKey getArtifactKey(ReactorProject project) {
        String id = project.getArtifactId();
        String version = getOsgiVersion(project);

        // TODO this is an invalid type constant for an ArtifactKey
        return new DefaultArtifactKey(PackagingType.TYPE_ECLIPSE_REPOSITORY, id, version);
    }

    @Override
    protected ArtifactDependencyWalker newDependencyWalker(ReactorProject project, TargetEnvironment environment) {
        final List<ProductConfiguration> products = loadProducts(project);
        final List<Category> categories = loadCategories(project);
        return new AbstractArtifactDependencyWalker(getDependencyArtifacts(project, environment),
                getEnvironments(project, environment)) {
            @Override
            public void walk(ArtifactDependencyVisitor visitor) {
                WalkbackPath visited = new WalkbackPath();
                for (ProductConfiguration product : products) {
                    traverseProduct(product, visitor, visited);
                }
                for (Category category : categories) {
                    for (FeatureRef feature : category.getFeatures()) {
                        traverseFeature(feature, visitor, visited);
                    }
                }
            }
        };
    }

    /**
     * Parses the category configuration files
     * 
     * @param project
     * @return
     */
    public List<Category> loadCategories(final ReactorProject project) {
        List<Category> categories = new ArrayList<>();
        for (File file : getCategoryFiles(project)) {
            try {
                categories.add(Category.read(file));
            } catch (IOException e) {
                throw new RuntimeException("Could not read product configuration file " + file.getAbsolutePath(), e);
            }
        }
        return categories;
    }

    /**
     * Parses the product configuration files
     * 
     * @param project
     * @return
     */
    protected List<ProductConfiguration> loadProducts(final ReactorProject project) {
        List<ProductConfiguration> products = new ArrayList<>();
        for (File file : getProductFiles(project)) {
            try {
                products.add(ProductConfiguration.read(file));
            } catch (IOException e) {
                throw new RuntimeException("Could not read product configuration file " + file.getAbsolutePath(), e);
            }
        }
        return products;
    }

    private List<File> getCategoryFiles(ReactorProject project) {
        List<File> res = new ArrayList<>();
        File categoryFile = new File(project.getBasedir(), "category.xml");
        if (categoryFile.exists()) {
            res.add(categoryFile);
        }
        return res;
    }

    /**
     * Looks for all files at the base of the project that extension is ".product" Duplicated in the
     * P2GeneratorImpl
     * 
     * @param project
     * @return The list of product files to parse for an eclipse-repository project
     */
    public List<File> getProductFiles(ReactorProject project) {
        File projectLocation = project.getBasedir();
        List<File> res = new ArrayList<>();
        for (File f : projectLocation.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".product") && !f.getName().startsWith(".polyglot")) {
                res.add(f);
            }
        }
        return res;
    }
}
