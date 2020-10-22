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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.osgi.framework.Version;

public interface EquinoxInstallationDescription {
    public static final Version EQUINOX_VERSION_3_3_0 = Version.parseVersion("3.3.0");

    public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    // introspection

    public List<ArtifactDescriptor> getBundles();

    public ArtifactDescriptor getSystemBundle();

    public ArtifactDescriptor getBundle(String symbolicName, String highestVersion);

    public List<File> getFrameworkExtensions();

    public Set<String> getBundlesToExplode();

    public Map<String, BundleStartLevel> getBundleStartLevel();

    public BundleStartLevel getDefaultBundleStartLevel();

    public Map<String, String> getPlatformProperties();

    public Map<String, String> getDevEntries();

    // mutators

    public void addBundle(ArtifactKey key, File basedir);

    public void addBundle(ArtifactKey key, File basedir, boolean override);

    public void addBundle(ArtifactDescriptor artifact);

    public void setDefaultBundleStartLevel(BundleStartLevel defaultBundleStartLevel);

    /**
     * This one is kinda odd, it reads bundle manifest to extract ArtifactKey.
     */
    // public void addBundle( File file, boolean override );

    public void addFrameworkExtensions(List<File> frameworkExtensions);

    public void addBundlesToExplode(List<String> bundlesToExplode);

    public void addBundleStartLevel(BundleStartLevel level);

    public void addPlatformProperty(String property, String value);

    public void addDevEntries(String id, String entries);
}
