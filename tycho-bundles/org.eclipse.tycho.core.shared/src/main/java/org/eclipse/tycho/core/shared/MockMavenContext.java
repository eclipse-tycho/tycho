/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.shared;

import java.io.File;
import java.util.Properties;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.PackagingType;

public class MockMavenContext extends MavenContextImpl {

    public MockMavenContext(File localRepositoryRoot, boolean offline, MavenLogger mavenLogger,
            Properties mergedProperties) {
        super(localRepositoryRoot, offline, mavenLogger, mergedProperties);
    }

    public MockMavenContext(File newFolder, MavenLogger logger) {
        super(newFolder, logger);
    }

    @Override
    public String getExtension(String artifactType) {
        if (artifactType == null) {
            return "jar";
        }
        switch (artifactType) {
        case ArtifactType.TYPE_ECLIPSE_PLUGIN:
        case ArtifactType.TYPE_ECLIPSE_FEATURE:
        case ArtifactType.TYPE_ECLIPSE_TEST_PLUGIN:
        case "ejb":
        case "ejb-client":
        case "test-jar":
        case "javadoc":
        case "java-source":
        case "maven-plugin":
            return "jar";
        case PackagingType.TYPE_ECLIPSE_UPDATE_SITE:
        case PackagingType.TYPE_ECLIPSE_REPOSITORY:
        case PackagingType.TYPE_ECLIPSE_APPLICATION:
        case ArtifactType.TYPE_ECLIPSE_PRODUCT:
            return "zip";
        case ArtifactType.TYPE_INSTALLABLE_UNIT:
            return "xml";
        case PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION:
            return "target";
        default:
            return "jar";
        }
    }

}
