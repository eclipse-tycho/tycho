/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
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
@Named(PackagingType.TYPE_ECLIPSE_REPOSITORY)
@Singleton
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

    @Override
    public void setupProject(MavenSession session, MavenProject project) {
        super.setupProject(session, project);
        //This is a hack for install plugin that requires a "main" artifact and otherwise fails
        //but a repository project may only attaches additional artifacts, e.g updatesite / products 
        Properties properties = project.getProperties();
        if (properties.getProperty("allowIncompleteProjects") == null) {
            properties.setProperty("allowIncompleteProjects", "true");
        }
    }

    /**
     * Parses the category configuration files
     *
     * @param project
     *            the project containing the category files
     * @return the parsed category configurations
     */
    public List<Category> loadCategories(final ReactorProject project) {
        return loadCategories(project.getBasedir());
    }

    /**
     * Parses the category configuration files
     *
     * @param categoriesDirectory
     *            the directory where the category files are stored
     * @return the parsed category configurations
     */
    public List<Category> loadCategories(final File categoriesDirectory) {
        final List<Category> categories = new ArrayList<>();

        for (final File file : getCategoryFiles(categoriesDirectory)) {
            try {
                categories.add(Category.read(file));
            } catch (final IOException e) {
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
    public static List<ProductConfiguration> loadProducts(final ReactorProject project) {
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

    private List<File> getCategoryFiles(final File basedir) {
        final List<File> files = new ArrayList<>();
        final File categoryFile = new File(basedir, "category.xml");

        if (categoryFile.exists()) {
            files.add(categoryFile);
        }

        return files;
    }

    /**
     * Looks for all files at the base of the project that extension is ".product" Duplicated in the
     * P2GeneratorImpl
     *
     * @param project
     *            the project containing the product files
     * @return The list of product files to parse for an eclipse-repository project
     */
    public static List<File> getProductFiles(final ReactorProject project) {
        final File projectLocation = project.getBasedir();
        return getProductFiles(projectLocation);
    }

    /**
     * Looks for all files with the extension ".product" under a specific directory.
     *
     * @param basedir
     *            the directory containing the product files
     * @return The list of product files to parse for an eclipse-repository project
     */
    public static List<File> getProductFiles(final File basedir) {
        final List<File> files = new ArrayList<>();

        // noinspection ConstantConditions
        for (final File file : basedir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".product") && !file.getName().startsWith(".polyglot")) {
                files.add(file);
            }
        }

        return files;
    }
}
