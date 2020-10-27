/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - refactor to combine the build directories
 *******************************************************************************/
package org.eclipse.tycho;

import java.io.File;

/**
 * Represents the build output directory of a Maven project (aka "target folder").
 */
public class BuildOutputDirectory implements BuildDirectory {

    private final File location;
    private final File outputDirectory;
    private final File testOutputDirectory;

    /**
     * @deprecated will be removed in next release!
     * @param location
     */
    @Deprecated
    public BuildOutputDirectory(String location) {
        this(new File(location));
    }

    public BuildOutputDirectory(File location) {
        this(location, null, null);
    }

    public BuildOutputDirectory(File location, File outputDirectory, File testOutputDirectory) {
        if (location == null) {
            throw new NullPointerException();
        }
        this.location = location;
        if (outputDirectory == null) {
            this.outputDirectory = new File(location, "classes");
        } else {
            this.outputDirectory = outputDirectory;
        }
        if (testOutputDirectory == null) {
            this.testOutputDirectory = new File(location, "test-classes");
        } else {
            this.testOutputDirectory = testOutputDirectory;
        }
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

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public File getTestOutputDirectory() {
        return testOutputDirectory;
    }

    @Override
    public File getP2AgentDirectory() {
        return getChild("p2agent");
    }
}
