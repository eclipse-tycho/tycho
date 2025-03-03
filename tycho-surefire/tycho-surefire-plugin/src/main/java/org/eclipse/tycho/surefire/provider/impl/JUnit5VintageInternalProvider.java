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

import static org.eclipse.tycho.surefire.provider.impl.ProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit5vintageinternal")
public class JUnit5VintageInternalProvider extends AbstractJUnit5Provider {

    private static final VersionRange JUNIT_VINTAGE_INTERNAL_VERSION_RANGE = new VersionRange("[5,5.12)");

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return List.of(newDependency("org.eclipse.tycho.surefire.junit5"),
                newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit5.vintage.internal"));
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        return isJUnit5(project, testBundleClassPath, JUNIT_VINTAGE_INTERNAL_VERSION_RANGE)
                && JUnit4Provider.isJUnit4(project, testBundleClassPath);
    }

    @Override
    public VersionRange getVersionRange() {
        return JUNIT_VINTAGE_INTERNAL_VERSION_RANGE;
    }
}
