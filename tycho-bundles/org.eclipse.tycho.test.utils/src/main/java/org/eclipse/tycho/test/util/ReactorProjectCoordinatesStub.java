/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
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
import org.eclipse.tycho.ReactorProjectCoordinates;

public class ReactorProjectCoordinatesStub implements ReactorProjectCoordinates {

    private static final String DUMMY_GROUP_ID = "dummy-group";
    private static final String DUMMY_ARTIFACT_ID = "dummy-artifact";
    private static final String DUMMY_VERSION = "0.1.2-SNAPSHOT";

    private BuildOutputDirectory targetFolder;

    public ReactorProjectCoordinatesStub(File outputFolder) {
        this.targetFolder = new BuildOutputDirectory(outputFolder);
    }

    public String getGroupId() {
        return DUMMY_GROUP_ID;
    }

    public String getArtifactId() {
        return DUMMY_ARTIFACT_ID;
    }

    public String getVersion() {
        return DUMMY_VERSION;
    }

    public BuildOutputDirectory getBuildDirectory() {
        return targetFolder;
    }
}
