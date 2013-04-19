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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = TestFrameworkProvider.class, hint = "junit47")
public class JUnit47Provider extends AbstractJUnitProvider {

    private static final Version VERSION = Version.parseVersion("4.7.0");

    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
        if (!isParallelEnabled(surefireProperties)) {
            return false;
        }
        return super.isEnabled(testBundleClassPath, surefireProperties);
    }

    private boolean isParallelEnabled(Properties providerProperties) {
        return providerProperties.getProperty("parallel") != null;
    }

    public String getSurefireProviderClassName() {
        return "org.apache.maven.surefire.junitcore.OsgiEnabledJUnitCoreProvider";
    }

    public Version getVersion() {
        return VERSION;
    }

    public List<Artifact> getRequiredBundles() {
        return singletonList((Artifact) new DefaultArtifact("org.eclipse.tycho", "org.eclipse.tycho.surefire.junit47",
                null, null));
    }

    @Override
    protected Set<String> getJUnitBundleNames() {
        return new HashSet<String>(asList("org.junit", "org.junit4"));
    }

    @Override
    protected VersionRange getJUnitVersionRange() {
        return new VersionRange("[4.7,5)");
    }

}
