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
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.eclipse.tycho.core.TargetEnvironment;

/**
 * @goal archive-products
 * @phase package
 */
public final class ProductArchiverMojo extends AbstractProductMojo {
    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="zip"
     */
    private Archiver inflater;

    /**
     * @component
     */
    private MavenProjectHelper helper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ProductConfig config = getProductConfig();
        if (!config.uniqueAttachIds()) {
            throw new MojoFailureException("Artifact file names for the archived products are not unique. "
                    + "Configure the attachId or select a subset of products. Current configuration: "
                    + config.getProducts());
        }
        for (Product product : config.getProducts()) {
            for (TargetEnvironment env : getEnvironments()) {
                File productArchive = new File(getProductsBuildDirectory(), getZipFileName(product) + "-"
                        + getOsWsArch(env, '.') + ".zip");

                try {
                    inflater.setDestFile(productArchive);
                    inflater.addDirectory(getProductMaterializeDirectory(product, env));
                    inflater.createArchive();
                } catch (ArchiverException e) {
                    throw new MojoExecutionException("Error packing product", e);
                } catch (IOException e) {
                    throw new MojoExecutionException("Error packing product", e);
                }

                final String artifactClassifier = getArtifactClassifier(product, env);
                helper.attachArtifact(getProject(), productArchive, artifactClassifier);
            }
        }
    }

    static String getZipFileName(Product product) {
        // overwrite output zip file name
        String name;
        if (product.getZipFileName() != null) {
            name = product.getZipFileName();
        } else {
            name = product.getId();
        }

        // include version number
        if (!product.isIncludeVersion()) {
            return name;
        } else {
            return name + "-" + product.getVersion();
        }
    }

    static String getArtifactClassifier(Product product, TargetEnvironment environment) {
        // classifier (and hence artifact file name) ends with os.ws.arch (similar to Eclipse
        // download packages)
        final String artifactClassifier;
        if (product.getAttachId() == null) {
            artifactClassifier = getOsWsArch(environment, '.');
        } else {
            artifactClassifier = product.getAttachId() + "-" + getOsWsArch(environment, '.');
        }
        return artifactClassifier;
    }
}
