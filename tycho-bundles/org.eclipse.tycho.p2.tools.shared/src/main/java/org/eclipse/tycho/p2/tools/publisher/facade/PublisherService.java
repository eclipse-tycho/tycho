/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher.facade;

import java.io.File;
import java.util.Collection;

import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.FacadeException;

public interface PublisherService {
    /**
     * Publishes given category definitions.
     * 
     * @param categoryDefinition
     *            A category.xml file as defined by the Eclipse PDE
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return handles to the root IUs in the publisher result
     */
    Collection<DependencySeed> publishCategories(File categoryDefinition) throws FacadeException;

    /**
     * Publishes the given OSGi execution environment profile file.
     * 
     * @param profileFile
     *            the .profile file
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return handles to the root IUs in the publisher result
     */
    Collection<DependencySeed> publishEEProfile(File profileFile) throws FacadeException;

    /**
     * Publishes the given OSGi execution environment profile.
     * 
     * @param profilename
     *            the profile name
     * @throws FacadeException
     *             if a checked exception occurs during publishing
     * @return handles to the root IUs in the publisher result
     */
    Collection<DependencySeed> publishEEProfile(String profileName) throws FacadeException;
}
