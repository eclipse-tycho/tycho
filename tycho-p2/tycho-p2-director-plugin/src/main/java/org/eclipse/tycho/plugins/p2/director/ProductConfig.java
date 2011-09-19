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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;

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

    public ProductConfig(List<Product> userConfig, File baseDir) throws MojoFailureException {
        if (userConfig != null) {
            products = userConfig;
            for (Product product : products) {
                if (product.getId() == null) {
                    throw new MojoFailureException("Attribute 'id' is required in POM product configuration");
                } else if (!new File(baseDir, product.getId()).isDirectory()) {
                    throw new MojoFailureException("Product id '" + product.getId() + "' not found");
                }
            }
        } else {
            /*
             * We assume that the tycho-p2-publisher-plugin has created folders named after the
             * product IDs (see
             * org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.prepareBuildProduct). This
             * is currently a limitation of this Maven plug-in - it can only be added to an
             * eclipse-repository module (which calls the tycho-p2-publisher-plugin).
             */
            File[] productIDs = baseDir.listFiles();
            products = new ArrayList<Product>(productIDs.length);
            for (File file : productIDs) {
                products.add(new Product(file.getName()));
            }
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
