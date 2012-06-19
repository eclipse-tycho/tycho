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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

@Component(role = TestFrameworkProvider.class, hint = "junit3")
public class JUnit3Provider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("3.8.0");

    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junit.JUnit3Provider";
    }

    public Version getVersion() {
        return VERSION;
    }

    public List<Artifact> getRequiredBundles() {
        return singletonList((Artifact) new DefaultArtifact("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit",
                null, null));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[3.8,4)");
    }

    @Override
    protected Set<String> getJUnitBundleNames() {
        return singleton("org.junit");
    }

}
