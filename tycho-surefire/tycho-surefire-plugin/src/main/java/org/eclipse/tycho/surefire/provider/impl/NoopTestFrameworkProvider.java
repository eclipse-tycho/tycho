/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.provider.impl;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.surefire.provider.spi.TestFrameworkProvider;
import org.osgi.framework.Version;

public final class NoopTestFrameworkProvider implements TestFrameworkProvider {

    @Override
    public String getType() {
        return "noop";
    }

    @Override
    public Version getVersion() {
        return Version.emptyVersion;
    }

    @Override
    public String getSurefireProviderClassName() {
        return "none";
    }

    @Override
    public boolean isEnabled(List<ClasspathEntry> testBundleClassPath, Properties providerProperties) {
        return false;
    }

    @Override
    public List<Dependency> getRequiredBundles() {
        return Collections.emptyList();
    }

    @Override
    public Properties getProviderSpecificProperties() {
        return new Properties();
    }

}
