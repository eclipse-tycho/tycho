/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.tycho;

import java.io.File;

public interface BuildDirectory {

    /**
     * Returns the location of the build output directory.
     * 
     * @return never <code>null</code>
     */
    File getLocation();

    /**
     * Convenience method for obtaining an file/folder in the build output directory.
     * 
     * @return The file or folder at the given <code>path</code> relative to the build output
     *         directory.
     */
    File getChild(String path);

    /**
     * 
     * @return the directory where compiled application classes are placed.
     */
    File getOutputDirectory();

    /**
     * @return the directory where compiled test classes are placed.
     */
    File getTestOutputDirectory();

    /**
     * 
     * @return the directory used for the P2 agent
     */
    File getP2AgentDirectory();
}
