/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.provider.impl;

import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

abstract class AbstractJUnit5Provider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("5");

    private static final Set<String> JUNIT5_BUNDLES = Set.of("org.junit.jupiter.api", "junit-jupiter-api");

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
    }

    static boolean isJUnit5(MavenProject project, List<ClasspathEntry> testBundleClassPath, VersionRange versionRange) {
        return isEnabled(project, testBundleClassPath, JUNIT5_BUNDLES, versionRange);
    }
}
