/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.ReactorProjectIdentities;

public class ReactorProjectIdentitiesStub extends ReactorProjectIdentities {

    private static final String DUMMY_GROUP_ID = "dummy-group";
    private static final String DUMMY_VERSION = "0.1.2-SNAPSHOT";

    private final File projectRoot;
    private final BuildOutputDirectory targetFolder;
    private final String artifactId;

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

    @Override
    public String getGroupId() {
        return DUMMY_GROUP_ID;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return DUMMY_VERSION;
    }

    @Override
    public File getBasedir() {
        return projectRoot;
    }

    @Override
    public BuildOutputDirectory getBuildDirectory() {
        return targetFolder;
    }

}
