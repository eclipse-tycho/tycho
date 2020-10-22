/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;

/**
 * Represents the build output directory of a Maven project (aka "target folder").
 */
public class BuildOutputDirectory {

    private final File location;

    public BuildOutputDirectory(File location) {
        if (location == null) {
            throw new NullPointerException();
        }
        this.location = location;
    }

    public BuildOutputDirectory(String directory) {
        this(new File(directory));
    }

    /**
     * Returns the location of the build output directory.
     * 
     * @return never <code>null</code>
     */
    public File getLocation() {
        return location;
    }

    /**
     * Convenience method for obtaining an file/folder in the build output directory.
     * 
     * @return The file or folder at the given <code>path</code> relative to the build output
     *         directory.
     */
    public File getChild(String path) {
        return new File(location, path);
    }
}
