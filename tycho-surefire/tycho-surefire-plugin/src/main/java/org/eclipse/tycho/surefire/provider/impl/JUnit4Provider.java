/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static org.eclipse.tycho.surefire.provider.impl.DefaultProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Provider for test bundles that only depend on JUnit 4 (and possibly JUnit 3 style tests). Since
 * Surefire 3.6.0 there is no dedicated JUnit 4 provider anymore, so the tests are executed by the
 * JUnit Platform provider using the JUnit Vintage engine, which must therefore be available in the
 * target platform.
 */
@Named("junit4")
public class JUnit4Provider extends AbstractJUnitProvider {

    private static final VersionRange JUNIT4_VERSION_RANGE = new VersionRange("[4,5)");
    private static final Version VERSION = Version.parseVersion("4");
    static final Set<String> JUNIT4_BUNDLES = Set.of("org.junit", "org.junit4");

    @Override
    public String getSurefireProviderClassName() {
        return AbstractJUnit5Provider.JUNIT_PLATFORM_PROVIDER;
    }

    @Override
    public Properties getProviderSpecificProperties() {
        Properties properties = new Properties();
        // Tells the JUnit Platform provider that groups/excludedGroups refer to JUnit 4 categories
        // and not to JUnit Platform tags (ProviderParameterNames.JUNIT_VINTAGE_DETECTED)
        properties.setProperty("junit.vintage.engine.detected", "true");
        return properties;
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return List.of(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit4"));
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        return isJUnit4(project, testBundleClassPath);
    }

    static boolean isJUnit4(MavenProject project, List<ClasspathEntry> testBundleClassPath) {
        return isEnabled(project, testBundleClassPath, JUNIT4_BUNDLES, JUNIT4_VERSION_RANGE);
    }

    @Override
    public VersionRange getVersionRange() {
        return JUNIT4_VERSION_RANGE;
    }

}
