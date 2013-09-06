/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - 386988 Support for provisioned applications
 ******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import static org.eclipse.osgi.framework.adaptor.FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;

/**
 * A "read-only" equinox installation (no bundles can be added, nothing configured). All
 * installation and configuration operations must handled separately, e.g. using p2 director.
 */
public class ProvisionedInstallationDescription implements EquinoxInstallationDescription {

    private File location;
    private ArtifactDescriptor systemBundleDescriptor;
    private BundleReader bundleReader;

    ProvisionedInstallationDescription(File location, BundleReader bundleReader) {
        this.location = location;
        this.bundleReader = bundleReader;
    }

    public ArtifactDescriptor getSystemBundle() {
        if (systemBundleDescriptor != null) {
            return systemBundleDescriptor;
        }
        File pluginsDir = new File(location, "plugins");
        File[] systemBundles = pluginsDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().startsWith(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + "_");
            }
        });
        File systemBundle;
        if (systemBundles.length == 0) {
            throw new IllegalArgumentException("No framework bundle " + FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME
                    + " found in " + pluginsDir);
        } else if (systemBundles.length > 1) {
            throw new IllegalArgumentException("Multiple versions of the framework bundle "
                    + FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + " found in " + pluginsDir);
        } else {
            systemBundle = systemBundles[0];
        }
        String version = bundleReader.loadManifest(systemBundle).getBundleVersion();
        ArtifactKey systemBundleKey = new DefaultArtifactKey(TYPE_ECLIPSE_PLUGIN, FRAMEWORK_SYMBOLICNAME, version);
        systemBundleDescriptor = new DefaultArtifactDescriptor(systemBundleKey, systemBundle, null, null, null);
        return systemBundleDescriptor;
    }

    public List<File> getFrameworkExtensions() {
        return Collections.emptyList();
    }

    public Set<String> getBundlesToExplode() {
        return Collections.emptySet();
    }

    public Map<String, BundleStartLevel> getBundleStartLevel() {
        return Collections.emptyMap();
    }

    public Map<String, String> getPlatformProperties() {
        return Collections.emptyMap();
    }

    public Map<String, String> getDevEntries() {
        return Collections.emptyMap();
    }

    public List<ArtifactDescriptor> getBundles() {
        throw new UnsupportedOperationException();
    }

    public ArtifactDescriptor getBundle(String symbolicName, String highestVersion) {
        throw new UnsupportedOperationException();
    }

    public void addBundle(ArtifactKey key, File basedir) {
        throw new UnsupportedOperationException();
    }

    public void addBundle(ArtifactKey key, File basedir, boolean override) {
        throw new UnsupportedOperationException();
    }

    public void addBundle(ArtifactDescriptor artifact) {
        throw new UnsupportedOperationException();
    }

    public void addFrameworkExtensions(List<File> frameworkExtensions) {
        throw new UnsupportedOperationException();
    }

    public void addBundlesToExplode(List<String> bundlesToExplode) {
        throw new UnsupportedOperationException();
    }

    public void addBundleStartLevel(BundleStartLevel level) {
        throw new UnsupportedOperationException();
    }

    public void addPlatformProperty(String property, String value) {
        throw new UnsupportedOperationException();
    }

    public void addDevEntries(String id, String entries) {
        throw new UnsupportedOperationException();
    }

}
