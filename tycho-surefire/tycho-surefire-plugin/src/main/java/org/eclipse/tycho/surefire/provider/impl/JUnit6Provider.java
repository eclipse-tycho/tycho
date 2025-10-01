/*******************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Foundation - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static java.util.Collections.singletonList;
import static org.eclipse.tycho.surefire.provider.impl.ProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit6")
public class JUnit6Provider extends AbstractJUnitProvider {

    private static final VersionRange JUNIT6_VERSION_RANGE = new VersionRange("[6,7)");
    private static final VersionRange JUNIT5_VERSION_RANGE = new VersionRange("[5,6)");

    private static final Version VERSION = Version.parseVersion("6");

    private static final Set<String> JUNIT6_BUNDLES = Set.of("org.junit.jupiter.api", "junit-jupiter-api");

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return singletonList(newDependency("org.eclipse.tycho.surefire.junit6"));
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        return isJUnit6(project, testBundleClassPath, JUNIT6_VERSION_RANGE)
                && !AbstractJUnit5Provider.isJUnit5(project, testBundleClassPath, JUNIT5_VERSION_RANGE)
                && !JUnit4Provider.isJUnit4(project, testBundleClassPath);
    }

    static boolean isJUnit6(MavenProject project, List<ClasspathEntry> testBundleClassPath, VersionRange versionRange) {
        return isEnabled(project, testBundleClassPath, JUNIT6_BUNDLES, versionRange);
    }

    @Override
    public VersionRange getVersionRange() {
        return JUNIT6_VERSION_RANGE;
    }

    @Override
    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
    }
}
