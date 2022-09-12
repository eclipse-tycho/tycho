/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.dotClasspath;

/**
 * represents a container classpath entry, this could be for example:
 * 
 * <ul>
 * <li>JRE Container (see {@link JREClasspathEntry#JRE_CONTAINER_PATH})</li>
 * <li>JUnit Container (see {@link JUnitClasspathContainerEntry#JUNIT_CONTAINER_PATH_PREFIX})</li>
 * <li>User Container (see {@link #USER_LIBRARY_PATH_PREFIX})</li>
 * <li>...</li>
 * </ul>
 * 
 */
public interface ClasspathContainerEntry extends ProjectClasspathEntry {

    static final String USER_LIBRARY_PATH_PREFIX = "org.eclipse.jdt.USER_LIBRARY/";

    /**
     * 
     * @return the path for this container
     */
    String getContainerPath();

}
