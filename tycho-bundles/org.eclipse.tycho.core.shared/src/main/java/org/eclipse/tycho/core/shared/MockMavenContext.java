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
import java.util.stream.Stream;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.MavenRepositoryLocation;
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
        return switch (artifactType) {
        case ArtifactType.TYPE_ECLIPSE_PLUGIN, ArtifactType.TYPE_ECLIPSE_FEATURE, ArtifactType.TYPE_ECLIPSE_TEST_PLUGIN, //
                "ejb", "ejb-client", "test-jar", "javadoc", "java-source", "maven-plugin" -> "jar";
        case PackagingType.TYPE_ECLIPSE_REPOSITORY, ArtifactType.TYPE_ECLIPSE_PRODUCT -> "zip";
        case ArtifactType.TYPE_INSTALLABLE_UNIT -> "xml";
        case PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION -> "target";
        default -> "jar";
        };
    }

    @Override
    public boolean isUpdateSnapshots() {
        return false;
    }

    @Override
    public Stream<MavenRepositoryLocation> getMavenRepositoryLocations() {
        return Stream.empty();
    }

    @Override
    public ChecksumPolicy getChecksumsMode() {
        return ChecksumPolicy.LAX;
    }

}
