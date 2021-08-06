/*******************************************************************************
 * Copyright (c) 2015, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products (gently sponsored by Compart AG)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher.facade;

import java.io.File;
import java.util.List;

import org.eclipse.tycho.core.resolver.shared.DependencySeed;

public interface PublishProductTool {

    /**
     * Publishes the given product definition.
     * 
     * @param productDefinition
     *            A .product file as defined by the Eclipse PDE
     * @param launcherBinaries
     *            A folder that contains the native Eclipse launcher binaries
     * @param flavor
     *            The installation flavor the product shall be published for
     * @return a handles to the published product IU
     */
    default List<DependencySeed> publishProduct(File productDefinition, File launcherBinaries, String flavor) {
        return publishProduct(productDefinition, launcherBinaries, flavor, false);
    }

    List<DependencySeed> publishProduct(File productDefinition, File launcherBinaries, String flavor,
            boolean ignorePluginConfigurations);
}
