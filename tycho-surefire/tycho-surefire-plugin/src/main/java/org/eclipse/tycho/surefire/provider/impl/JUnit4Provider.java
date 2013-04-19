/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.surefire.provider.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.eclipse.tycho.surefire.provider.impl.ProviderSelector.newDependency;

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
        return new HashSet<String>(asList("org.junit", "org.junit4"));
    }

    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junit4.JUnit4Provider";
    }

    public Version getVersion() {
        return VERSION;
    }

    public List<Dependency> getRequiredBundles() {
        return singletonList(newDependency("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit4"));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[4,5)");
    }

}
