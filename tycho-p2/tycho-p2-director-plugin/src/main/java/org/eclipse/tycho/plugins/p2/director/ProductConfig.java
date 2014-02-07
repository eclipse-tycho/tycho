/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;

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

    public ProductConfig(List<Product> userConfig, Collection<DependencySeed> projectSeeds) throws MojoFailureException {
        if (userConfig != null) {
            products = userConfig;
            for (Product product : products) {
                checkConfiguredProductsExist(product, projectSeeds);
            }
        } else {
            // no product ID specified -> if a product has been published, use that one
            products = usePublishedProduct(projectSeeds);
        }
    }

    private static void checkConfiguredProductsExist(Product configuredProduct, Collection<DependencySeed> projectSeeds)
            throws MojoFailureException {

        if (configuredProduct.getId() == null) {
            throw new MojoFailureException("Attribute 'id' is required in POM product configuration");

        } else {
            // look for product with the configured ID in the publishing result
            // TODO also look in the target platform
            for (DependencySeed seed : projectSeeds) {
                if (ArtifactKey.TYPE_ECLIPSE_PRODUCT.equals(seed.getType())
                        && configuredProduct.getId().equals(seed.getId())) {
                    return;
                }
            }
            throw new MojoFailureException("Product with id '" + configuredProduct.getId()
                    + "' does not exist in the project"); // TODO "... in the target platform"
        }
    }

    private static List<Product> usePublishedProduct(Collection<DependencySeed> projectSeeds) {
        List<Product> result = new ArrayList<Product>(1);

        // publishing results are added to the dependency seeds of the project, so we can find the products there
        for (DependencySeed seed : projectSeeds) {

            if (ArtifactKey.TYPE_ECLIPSE_PRODUCT.equals(seed.getType())) {
                result.add(new Product(seed.getId()));
                // if there is more than one published product, the uniqueAttachIds() check will fail later on
            }
        }
        return result;
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
