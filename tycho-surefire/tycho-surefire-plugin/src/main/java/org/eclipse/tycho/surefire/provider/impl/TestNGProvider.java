/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.surefire.provider.impl;

import static org.eclipse.tycho.surefire.provider.impl.DefaultProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Provider for TestNG test bundles. Since Surefire 3.6.0 there is no dedicated TestNG provider
 * anymore, so the tests are executed by the JUnit Platform provider using the
 * <a href="https://github.com/junit-team/testng-engine">TestNG Engine for the JUnit Platform</a>
 * that is embedded in the {@code org.eclipse.tycho.surefire.testng} fragment. The JUnit Platform
 * (launcher) must therefore be available in the target platform.
 */
@Named("testng")
public class TestNGProvider implements TestFrameworkProvider {

    private static final String TESTNG_BSN = "org.testng";
    /**
     * Minimum TestNG version supported by the TestNG Engine for the JUnit Platform
     */
    private static final Version VERSION = Version.parseVersion("6.14.3");
    private static final VersionRange VERSION_RANGE = new VersionRange("[6.14.3,8)");

    @Override
    public String getType() {
        return "testng";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public String getSurefireProviderClassName() {
        return AbstractJUnit5Provider.JUNIT_PLATFORM_PROVIDER;
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        for (ClasspathEntry classpathEntry : testBundleClassPath) {
            ArtifactKey artifactKey = classpathEntry.getArtifactKey();
            if (TESTNG_BSN.equals(artifactKey.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return List.of(newDependency("org.eclipse.tycho.surefire.testng"),
                newDependency("org.eclipse.tycho.surefire.testng.fixup"));
    }

    @Override
    public Properties getProviderSpecificProperties() {
        Properties properties = new Properties();
        // The JUnit Platform provider only checks the presence of this property to decide that
        // TestNG is used, e.g. to pass groups/excludedGroups to the TestNG engine instead of
        // applying them as JUnit Platform tag filters
        properties.setProperty("testng.version", VERSION.toString());
        return properties;
    }

    @Override
    public VersionRange getVersionRange() {
        return VERSION_RANGE;
    }

}
