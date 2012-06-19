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

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public abstract class AbstractJUnitProvider implements TestFrameworkProvider {

    public AbstractJUnitProvider() {
    }

    public String getType() {
        return "junit";
    }

    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties surefireProperties) {
        Set<String> junitBundleNames = getJUnitBundleNames();
        VersionRange range = getJUnitVersionRange();
        for (ClasspathEntry classpathEntry : testBundleClassPath) {
            ArtifactKey artifactKey = classpathEntry.getArtifactKey();
            if (junitBundleNames.contains(artifactKey.getId())) {
                Version version = Version.parseVersion(artifactKey.getVersion());
                if (range.includes(version)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract VersionRange getJUnitVersionRange();

    protected abstract Set<String> getJUnitBundleNames();

}
