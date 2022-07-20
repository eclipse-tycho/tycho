/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FrameworkInstallationDescription {

    public Collection<BundleReference> getBundles();

    public BundleReference getSystemBundle();

    public BundleReference getBundle(String symbolicName, String highestVersion);

    public Collection<File> getFrameworkExtensions();

    public Set<String> getBundlesToExplode();

    public Map<String, BundleStartLevel> getBundleStartLevel();

    public BundleStartLevel getDefaultBundleStartLevel();

    public Map<String, String> getPlatformProperties();

    public Map<String, String> getDevEntries();

    default void addBundle(String id, String version, File location) {
        addBundle(new BundleReference() {

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public File getLocation() {
                return location;
            }

            @Override
            public String getId() {
                return id;
            }
        });
    }

    void addBundle(BundleReference key);

    void setDefaultBundleStartLevel(BundleStartLevel defaultBundleStartLevel);

    void addFrameworkExtensions(List<File> frameworkExtensions);

    void addBundlesToExplode(List<String> bundlesToExplode);

    void addBundleStartLevel(BundleStartLevel level);

    void addPlatformProperty(String property, String value);

    void addDevEntries(String id, String entries);
}
