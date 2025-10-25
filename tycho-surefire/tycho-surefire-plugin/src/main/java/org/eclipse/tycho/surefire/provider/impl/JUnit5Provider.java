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

import static org.eclipse.tycho.surefire.provider.impl.DefaultProviderHelper.newDependency;

import java.util.List;
import java.util.Properties;

import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ClasspathEntry;
import org.osgi.framework.VersionRange;

@Named("junit5")
public class JUnit5Provider extends AbstractJUnit5Provider {

    private static final VersionRange JUNIT5_VERSION_RANGE = new VersionRange("[5,6)");

    @Override
    public List<Dependency> getRequiredArtifacts() {
        return List.of(newDependency("org.eclipse.tycho.surefire.junit5"));
    }

    @Override
    public boolean isEnabled(MavenProject project, List<ClasspathEntry> testBundleClassPath,
            Properties surefireProperties) {
        return isJUnit5(project, testBundleClassPath, JUNIT5_VERSION_RANGE)
                && !JUnit4Provider.isJUnit4(project, testBundleClassPath);
    }

    @Override
    public VersionRange getVersionRange() {
        return JUNIT5_VERSION_RANGE;
    }
}
