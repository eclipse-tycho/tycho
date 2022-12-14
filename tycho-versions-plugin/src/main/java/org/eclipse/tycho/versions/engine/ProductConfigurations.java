/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
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

package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tycho.model.ProductConfiguration;

/**
 * Value object to hold multiple product files in eclipse-repository projects registered in
 * {@link ProjectMetadata}.
 */
public class ProductConfigurations {

    private Map<File, ProductConfiguration> productConfigurations = new HashMap<>();

    public void addProductConfiguration(File productFile, ProductConfiguration productConfiguration) {
        productConfigurations.put(productFile, productConfiguration);
    }

    public Map<File, ProductConfiguration> getProductConfigurations() {
        return productConfigurations;
    }
}
