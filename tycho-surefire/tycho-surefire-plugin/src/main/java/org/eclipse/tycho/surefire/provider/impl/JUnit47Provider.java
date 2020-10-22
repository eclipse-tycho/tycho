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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.eclipse.tycho.surefire.provider.impl.ProviderHelper.newDependency;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit47")
public class JUnit47Provider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("4.7.0");

    @Override
    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
        if (hasGroups(surefireProperties)) {
            return true;
        }
        if (!isParallelEnabled(surefireProperties)) {
            return false;
        }
        return super.isEnabled(testBundleClassPath, surefireProperties);
    }

    private boolean hasGroups(Properties providerProperties) {
        return providerProperties.getProperty(ProviderParameterNames.TESTNG_GROUPS_PROP) != null
                || providerProperties.getProperty(ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP) != null;
    }

    private boolean isParallelEnabled(Properties providerProperties) {
        return providerProperties.getProperty("parallel") != null;
    }

    @Override
    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junitcore.JUnitCoreProvider";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public List<Dependency> getRequiredBundles() {
        return singletonList(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit47"));
    }

    @Override
    protected Set<String> getJUnitBundleNames() {
        return new HashSet<>(asList("org.junit", "org.junit4"));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[4.7,5)");
    }

}
