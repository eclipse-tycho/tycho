/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 ******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.sisu.equinox.launching.BundleReference;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.FrameworkInstallationDescription;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.BundleReader;

/**
 * A "read-only" equinox installation (no bundles can be added, nothing configured). All
 * installation and configuration operations must handled separately, e.g. using p2 director.
 */
public class ProvisionedInstallationDescription implements FrameworkInstallationDescription {

    private File location;
    private BundleReference systemBundleDescriptor;
    private BundleReader bundleReader;

    ProvisionedInstallationDescription(File location, BundleReader bundleReader) {
        this.location = location;
        this.bundleReader = bundleReader;
    }

    @Override
    public BundleReference getSystemBundle() {
        if (systemBundleDescriptor != null) {
            return systemBundleDescriptor;
        }
        File pluginsDir = new File(location, "plugins");
        File[] systemBundles = pluginsDir.listFiles(
                (FileFilter) file -> file.isFile() && file.getName().startsWith(EquinoxContainer.NAME + "_"));
        File systemBundle;
        if (systemBundles.length == 0) {
            throw new IllegalArgumentException(
                    "No framework bundle " + EquinoxContainer.NAME + " found in " + pluginsDir);
        } else if (systemBundles.length > 1) {
            throw new IllegalArgumentException(
                    "Multiple versions of the framework bundle " + EquinoxContainer.NAME + " found in " + pluginsDir);
        } else {
            systemBundle = systemBundles[0];
        }
        String version = bundleReader.loadManifest(systemBundle).getBundleVersion();
        ArtifactKey systemBundleKey = new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, EquinoxContainer.NAME,
                version);
        systemBundleDescriptor = new BundleReference() {

            @Override
            public String getVersion() {
                return systemBundleKey.getVersion();
            }

            @Override
            public File getLocation() {
                return systemBundle;
            }

            @Override
            public String getId() {
                return systemBundleKey.getId();
            }
        };
        return systemBundleDescriptor;
    }

    @Override
    public List<File> getFrameworkExtensions() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getBundlesToExplode() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, BundleStartLevel> getBundleStartLevel() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getPlatformProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getDevEntries() {
        return Collections.emptyMap();
    }

    @Override
    public List<BundleReference> getBundles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleReference getBundle(String symbolicName, String highestVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addBundle(BundleReference reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFrameworkExtensions(List<File> frameworkExtensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addBundlesToExplode(List<String> bundlesToExplode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addBundleStartLevel(BundleStartLevel level) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPlatformProperty(String property, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDevEntries(String id, String entries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleStartLevel getDefaultBundleStartLevel() {
        return null;
    }

    @Override
    public void setDefaultBundleStartLevel(BundleStartLevel defaultBundleStartLevel) {
        throw new UnsupportedOperationException();
    }

}
