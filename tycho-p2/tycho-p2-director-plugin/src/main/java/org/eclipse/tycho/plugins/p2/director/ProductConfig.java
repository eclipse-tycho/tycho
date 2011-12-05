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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.model.ProductConfiguration;

/**
 * Effective product configuration for this Maven plug-in. This is the configuration provided by the
 * user (in the <configuration> section), or the product file in the root directory of the module if
 * no user configuration is provided.
 * <p>
 * Note: Since the attachId defaults to <code>null</code>, explicit configuration must be provided
 * if there is more than one product file.
 * </p>
 */
class ProductConfig {
    private List<Product> products;

    public ProductConfig(List<Product> userConfig, File buildBaseDir, File projectBaseDir) throws MojoFailureException {
        if (userConfig != null) {
            products = userConfig;
            for (Product product : products) {
                if (product.getId() == null) {
                    throw new MojoFailureException("Attribute 'id' is required in POM product configuration");
                } else if (!new File(buildBaseDir, product.getId()).isDirectory()) {
                    throw new MojoFailureException("Product id '" + product.getId() + "' not found");
                }
                setProductVersion(product, projectBaseDir);
            }
        } else {
            /*
             * We assume that the tycho-p2-publisher-plugin has created folders named after the
             * product IDs (see
             * org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.prepareBuildProduct). This
             * is currently a limitation of this Maven plug-in - it can only be added to an
             * eclipse-repository module (which calls the tycho-p2-publisher-plugin).
             */
            if (buildBaseDir.exists()) {
                File[] productIDs = buildBaseDir.listFiles();
                products = new ArrayList<Product>(productIDs.length);
                for (File file : productIDs) {
                    Product product = new Product(file.getName());
                    setProductVersion(product, projectBaseDir);
                    products.add(product);
                }
            } else {
                // the product publisher did not create the basedir. So there was no project definition file. Nothing to do.
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=356716
                products = Collections.emptyList();
            }
        }
    }

    private void setProductVersion(Product product, File projectBaseDir) {
        File file = new File(projectBaseDir, product.getId() + ".product");
        try {
            ProductConfiguration productConfiguration = ProductConfiguration.read(file);
            String version = productConfiguration.getVersion();
            if (version.endsWith(".qualifier")) {
                version = version.substring(0, version.length() - 10);
            }
            product.setVersion(version);
        } catch (IOException e) {
            System.err.println("product not found " + projectBaseDir + File.separator + file.getName());
            // ignore
        }
    }

    public boolean uniqueAttachIds() {
        Set<String> attachIDs = new HashSet<String>();
        for (Product product : products) {
            if (!attachIDs.contains(product.getAttachId())) {
                attachIDs.add(product.getAttachId());
            } else {
                return false;
            }
        }
        return true;
    }

    public List<Product> getProducts() {
        return products;
    }

}
