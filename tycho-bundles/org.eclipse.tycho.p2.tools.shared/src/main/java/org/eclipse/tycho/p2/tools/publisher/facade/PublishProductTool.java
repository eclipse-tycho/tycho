/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher.facade;

import java.io.File;
import java.util.Collection;

import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.FacadeException;

public interface PublishProductTool {

    // TODO update javadoc
    /**
     * Publishes the given product definition.
     * 
     * 
     * @param productDefinition
     *            A .product file as defined by the Eclipse PDE
     * @param launcherBinaries
     *            A folder that contains the native Eclipse launcher binaries
     * @param flavor
     *            The installation flavor the product shall be published for
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return handles to the root IUs in the publisher result
     */
    Collection<DependencySeed> publishProduct(File productDefinition, File launcherBinaries, String flavor)
            throws FacadeException;

}
