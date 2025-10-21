/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import java.net.URI;

import org.apache.maven.plugin.Mojo;

/**
 * The {@link EclipseWorkspaceManager} manages dedicated workspaces on a per thread basis using a
 * key object
 */
public interface EclipseWorkspaceManager {

    /**
     * @param key
     *            the key to use
     * @return a workspace directory that can be used by the current thread.
     */
    <T> EclipseWorkspace<T> getWorkspace(T key);

    /**
     * Get a workspace that is unique for the given uri, current thread and mojo and therefore safe
     * to be used in a maven multithread execution
     * 
     * @param uri
     * @param mojo
     * @return an {@link EclipseWorkspace}
     */
    EclipseWorkspace<?> getWorkspace(URI uri, Mojo mojo);

}
