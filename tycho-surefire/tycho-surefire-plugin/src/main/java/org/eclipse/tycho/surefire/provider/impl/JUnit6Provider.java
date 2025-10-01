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

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit6")
public class JUnit6Provider extends AbstractJUnit5Provider {

    private static final VersionRange JUNIT6_VERSION_RANGE = new VersionRange("[6,7)");

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return singletonList(newDependency("org.eclipse.tycho.surefire.junit6"));
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        return isJUnit5(project, testBundleClassPath, JUNIT6_VERSION_RANGE)
                && !JUnit4Provider.isJUnit4(project, testBundleClassPath);
    }

    @Override
    public VersionRange getVersionRange() {
        return JUNIT6_VERSION_RANGE;
    }
}
