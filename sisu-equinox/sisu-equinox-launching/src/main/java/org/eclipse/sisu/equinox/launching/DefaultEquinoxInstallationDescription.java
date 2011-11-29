/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;

public class DefaultEquinoxInstallationDescription implements EquinoxInstallationDescription {
    private static final Map<String, BundleStartLevel> DEFAULT_START_LEVEL = new HashMap<String, BundleStartLevel>();

    static {
        setDefaultStartLevel("org.eclipse.equinox.common", 2);
        setDefaultStartLevel("org.eclipse.core.runtime", 4);
        setDefaultStartLevel("org.eclipse.equinox.simpleconfigurator", 1);
        setDefaultStartLevel("org.eclipse.update.configurator", 3);
        setDefaultStartLevel("org.eclipse.osgi", -1);
        setDefaultStartLevel("org.eclipse.equinox.ds", 1);
    }

    private static void setDefaultStartLevel(String id, int level) {
        DEFAULT_START_LEVEL.put(id, new BundleStartLevel(id, level, true));
    }

    protected final DefaultTargetPlatform bundles = new DefaultTargetPlatform();

    private final Map<String, BundleStartLevel> startLevel = new HashMap<String, BundleStartLevel>(DEFAULT_START_LEVEL);

    private final List<File> frameworkExtensions = new ArrayList<File>();

    private final Set<String> bundlesToExplode = new HashSet<String>();

    public void addBundleStartLevel(BundleStartLevel level) {
        startLevel.put(level.getId(), level);
    }

    public Map<String, BundleStartLevel> getBundleStartLevel() {
        return startLevel;
    }

    public ArtifactDescriptor getBundle(String symbolicName, String highestVersion) {
        return bundles.getArtifact(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, symbolicName, highestVersion);
    }

    public List<ArtifactDescriptor> getBundles() {
        return bundles.getArtifacts(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN);
    }

    public ArtifactDescriptor getSystemBundle() {
        return bundles.getArtifact(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN,
                FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, null);
    }

    public void addBundle(ArtifactDescriptor artifact) {
        bundles.addArtifact(artifact);
    }

    public void addBundle(ArtifactKey key, File file) {
        addBundle(key, file, false);
    }

    public void addBundle(ArtifactKey key, File file, boolean override) {
        if (override) {
            bundles.removeAll(key.getType(), key.getId());
        }

        bundles.addArtifactFile(key, file, null);
    }

    public void addBundlesToExplode(List<String> bundlesToExplode) {
        this.bundlesToExplode.addAll(bundlesToExplode);
    }

    public Set<String> getBundlesToExplode() {
        return bundlesToExplode;
    }

    public void addFrameworkExtensions(List<File> frameworkExtensions) {
        this.frameworkExtensions.addAll(frameworkExtensions);
    }

    public List<File> getFrameworkExtensions() {
        return frameworkExtensions;
    }
}
