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
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit4")
public class JUnit4Provider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("4.0.0");

    @Override
    protected Set<String> getJUnitBundleNames() {
        return new HashSet<>(asList("org.junit", "org.junit4"));
    }

    @Override
    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junit4.JUnit4Provider";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public List<Dependency> getRequiredBundles() {
        return singletonList(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit4"));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[4,5)");
    }

}
