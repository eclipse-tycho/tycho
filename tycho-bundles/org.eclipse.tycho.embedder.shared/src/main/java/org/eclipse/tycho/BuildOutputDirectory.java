/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
