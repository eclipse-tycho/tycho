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

import static java.util.Collections.singletonList;
import static org.eclipse.tycho.surefire.provider.impl.ProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;

@Component(role = TestFrameworkProvider.class, hint = "testng")
public class TestNGProvider implements TestFrameworkProvider {

    private static final String TESTNG_BSN = "org.testng";
    private static final Version VERSION = Version.parseVersion("6.9.10");

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
        return "org.apache.maven.surefire.testng.TestNGProvider";
    }

    @Override
    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
        for (ClasspathEntry classpathEntry : testBundleClassPath) {
            ArtifactKey artifactKey = classpathEntry.getArtifactKey();
            if (TESTNG_BSN.equals(artifactKey.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Dependency> getRequiredBundles() {
        return singletonList(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.testng"));
    }

    @Override
    public Properties getProviderSpecificProperties() {
        Properties properties = new Properties();
        properties.setProperty("testng.configurator", "org.apache.maven.surefire.testng.conf.TestNG60Configurator");
        return properties;
    }

}
