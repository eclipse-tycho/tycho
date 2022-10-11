/*******************************************************************************
 * Copyright (c) 2012, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;

import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectIdentities;

public class ReactorProjectIdentitiesStub extends ReactorProjectIdentities {

    private static final String DUMMY_GROUP_ID = "dummy-group";
    private static final String DUMMY_VERSION = "0.1.2-SNAPSHOT";

    private final File projectRoot;
    private final BuildDirectory targetFolder;
    private final String artifactId;

    /**
     * Creates a dummy {@link ReactorProjectIdentities} instance with a GAV with the given
     * artifactId. The project root and build directory members remain undefined.
     */
    public ReactorProjectIdentitiesStub(String artifactId) {
        this.artifactId = artifactId;

        this.projectRoot = null;
        this.targetFolder = null;
    }

    /**
     * Creates a dummy {@link ReactorProjectIdentities} instance with the given directory as project
     * root, the "target" sub-folder as build output directory, and the last segment of the project
     * root path as artifactId.
     */
    public ReactorProjectIdentitiesStub(File projectRoot) {
        this(projectRoot, projectRoot.getName());
    }

    public ReactorProjectIdentitiesStub(File projectRoot, String artifactId) {
        this.projectRoot = projectRoot;
        this.targetFolder = new BuildOutputDirectory(new File(projectRoot, "target"));
        this.artifactId = artifactId;

        this.targetFolder.getLocation().mkdirs();
    }

    private static <T> T unlessNull(T value) {
        if (value == null)
            throw new UnsupportedOperationException();
        else
            return value;
    }

    @Override
    public String getGroupId() {
        return DUMMY_GROUP_ID;
    }

    @Override
    public String getArtifactId() {
        return unlessNull(artifactId);
    }

    @Override
    public String getVersion() {
        return DUMMY_VERSION;
    }

    @Override
    public File getBasedir() {
        return unlessNull(projectRoot);
    }

    @Override
    public BuildDirectory getBuildDirectory() {
        return unlessNull(targetFolder);
    }

}
