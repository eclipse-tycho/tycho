/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
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
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit58withvintage")
public class JUnit58WithVintageProvider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("5.8.0");

    @Override
    protected Set<String> getJUnitBundleNames() {
        return new HashSet<>(asList("org.junit.jupiter.api"));
    }

    @Override
    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public List<Dependency> getRequiredBundles() {
        return singletonList(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit58withvintage"));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[5.8,6.0)");
    }

    @Override
    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
        return super.isEnabled(testBundleClassPath, surefireProperties)
                && new JUnit47Provider().containsJunitInClasspath(testBundleClassPath);
    }
}
