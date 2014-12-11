/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
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

import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;

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

    protected final DefaultDependencyArtifacts bundles = new DefaultDependencyArtifacts();

    private final Map<String, BundleStartLevel> startLevel = new HashMap<String, BundleStartLevel>(DEFAULT_START_LEVEL);

    private BundleStartLevel defaultBundleStartLevel = null;

    private final List<File> frameworkExtensions = new ArrayList<File>();

    private final Set<String> bundlesToExplode = new HashSet<String>();

    private final Map<String, String> platformProperties = new HashMap<String, String>();

    private final Map<String, String> devEntries = new HashMap<String, String>();

    @Override
    public void addBundleStartLevel(BundleStartLevel level) {
        startLevel.put(level.getId(), level);
    }

    @Override
    public Map<String, BundleStartLevel> getBundleStartLevel() {
        return startLevel;
    }

    @Override
    public BundleStartLevel getDefaultBundleStartLevel() {
        return defaultBundleStartLevel;
    }

    @Override
    public void setDefaultBundleStartLevel(BundleStartLevel defaultBundleStartLevel) {
        this.defaultBundleStartLevel = defaultBundleStartLevel;
    }

    @Override
    public ArtifactDescriptor getBundle(String symbolicName, String highestVersion) {
        return bundles.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, symbolicName, highestVersion);
    }

    @Override
    public List<ArtifactDescriptor> getBundles() {
        return bundles.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
    }

    @Override
    public ArtifactDescriptor getSystemBundle() {
        return bundles.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN, EquinoxContainer.NAME, null);
    }

    @Override
    public void addBundle(ArtifactDescriptor artifact) {
        bundles.addArtifact(artifact);
    }

    @Override
    public void addBundle(ArtifactKey key, File file) {
        addBundle(key, file, false);
    }

    @Override
    public void addBundle(ArtifactKey key, File file, boolean override) {
        if (override) {
            bundles.removeAll(key.getType(), key.getId());
        }

        bundles.addArtifactFile(key, file, null);
    }

    @Override
    public void addBundlesToExplode(List<String> bundlesToExplode) {
        this.bundlesToExplode.addAll(bundlesToExplode);
    }

    @Override
    public Set<String> getBundlesToExplode() {
        return bundlesToExplode;
    }

    @Override
    public void addFrameworkExtensions(List<File> frameworkExtensions) {
        this.frameworkExtensions.addAll(frameworkExtensions);
    }

    @Override
    public List<File> getFrameworkExtensions() {
        return frameworkExtensions;
    }

    @Override
    public void addPlatformProperty(String property, String value) {
        platformProperties.put(property, value);
    }

    @Override
    public Map<String, String> getPlatformProperties() {
        return platformProperties;
    }

    @Override
    public void addDevEntries(String id, String entries) {
        if (entries != null) {
            devEntries.put(id, entries);
        }
    }

    @Override
    public Map<String, String> getDevEntries() {
        return devEntries;
    }
}
