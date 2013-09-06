/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. (mistria)- 386988 Support for provisionned applications
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;

/**
 * This class provides an implementation of an {@link EquinoxInstallation} which represents a fully
 * provisioned RCP application on file system.
 * 
 * @author mistria
 * 
 */
public class ProvisionedEquinoxInstallation implements EquinoxInstallation {

    public class ProvisionnedInstallationDescription implements EquinoxInstallationDescription {

        protected ArtifactDescriptor systemBundle;

        public List<ArtifactDescriptor> getBundles() {
            return null;
        }

        public ArtifactDescriptor getSystemBundle() {
            if (this.systemBundle == null) {
                File[] bundles = new File(ProvisionedEquinoxInstallation.this.location, "plugins")
                        .listFiles(new FilenameFilter() {
                            public boolean accept(File file, String name) {
                                return name.startsWith(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + "_");
                            }
                        });
                // TODO don't rely on unspecified filesystem ordering
                File systemBundleFile = bundles[bundles.length - 1]; // Use latest version (arbitrary)
                String version = systemBundleFile.getName()
                        .substring(0, systemBundleFile.getName().length() - ".jar".length()).split("_")[1];
                ArtifactKey systemBundleKey = new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN,
                        FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, version);
                this.systemBundle = new DefaultArtifactDescriptor(systemBundleKey, systemBundleFile, null, null, null);
            }
            return this.systemBundle;
        }

        // TODO check if all the generated stubs below are OK/required at all 
        public ArtifactDescriptor getBundle(String symbolicName, String highestVersion) {
            // TODO Auto-generated method stub
            return null;
        }

        public List<File> getFrameworkExtensions() {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<String> getBundlesToExplode() {
            // TODO Auto-generated method stub
            return null;
        }

        public Map<String, BundleStartLevel> getBundleStartLevel() {
            // TODO Auto-generated method stub
            return null;
        }

        public Map<String, String> getPlatformProperties() {
            // TODO Auto-generated method stub
            return null;
        }

        public Map<String, String> getDevEntries() {
            // TODO Auto-generated method stub
            return null;
        }

        public void addBundle(ArtifactKey key, File basedir) {
            // TODO Auto-generated method stub

        }

        public void addBundle(ArtifactKey key, File basedir, boolean override) {
            // TODO Auto-generated method stub

        }

        public void addBundle(ArtifactDescriptor artifact) {
            // TODO Auto-generated method stub

        }

        public void addFrameworkExtensions(List<File> frameworkExtensions) {
            // TODO Auto-generated method stub

        }

        public void addBundlesToExplode(List<String> bundlesToExplode) {
            // TODO Auto-generated method stub

        }

        public void addBundleStartLevel(BundleStartLevel level) {
            // TODO Auto-generated method stub

        }

        public void addPlatformProperty(String property, String value) {
            // TODO Auto-generated method stub

        }

        public void addDevEntries(String id, String entries) {
            // TODO Auto-generated method stub

        }

    }

    private File location;
    private File launcherJar;
    private EquinoxInstallationDescription description;

    public ProvisionedEquinoxInstallation(File applicationLocation) {
        this.location = applicationLocation;
    }

    public File getLauncherJar() {
        if (this.launcherJar != null) {
            return this.launcherJar;
        }
        File[] launcherJars = new File(getLocation(), "plugins").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("org.eclipse.equinox.launcher_");
            }
        });
        if (launcherJars.length > 0) {
            this.launcherJar = launcherJars[0];
        }
        return this.launcherJar;
    }

    public File getLocation() {
        return this.location;
    }

    public File getConfigurationLocation() {
        return new File(getLocation(), "configuration"); //$NON-NLS-1$
    }

    public EquinoxInstallationDescription getInstallationDescription() {
        if (this.description == null) {
            this.description = new ProvisionnedInstallationDescription();
        }
        return this.description;
    }

}
