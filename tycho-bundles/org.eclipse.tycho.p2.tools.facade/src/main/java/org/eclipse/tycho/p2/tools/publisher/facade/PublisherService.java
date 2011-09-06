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
package org.eclipse.tycho.p2.tools.publisher.facade;

import java.io.File;
import java.util.Collection;

import org.eclipse.tycho.p2.tools.FacadeException;

public interface PublisherService {
    /**
     * Publishes given category definitions.
     * 
     * @param categoryDefinition
     *            A category.xml file as defined by the Eclipse PDE
     * @throws IllegalStateException
     *             if this service instance has already been stopped
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return the root IUs in the publisher result
     */
    Collection</* IInstallableUnit */?> publishCategories(File categoryDefinition) throws FacadeException,
            IllegalStateException;

    /**
     * Publishes the given product definition.
     * 
     * @param productDefinition
     *            A .product file as defined by the Eclipse PDE
     * @param launcherBinaries
     *            A folder that contains the native Eclipse launcher binaries
     * @param flavor
     *            The installation flavor the product shall be published for
     * @throws IllegalStateException
     *             if this service instance has already been stopped
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return the root IUs in the publisher result
     */
    Collection</* IInstallableUnit */?> publishProduct(File productDefinition, File launcherBinaries, String flavor)
            throws FacadeException, IllegalStateException;

    /**
     * Stops this PublisherService instance. This releases internally used resources.
     */
    void stop();
}
