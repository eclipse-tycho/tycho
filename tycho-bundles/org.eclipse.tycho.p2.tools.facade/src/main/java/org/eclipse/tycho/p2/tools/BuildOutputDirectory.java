/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools;

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

    public File getChild(String path) {
        return new File(location, path);
    }
}
