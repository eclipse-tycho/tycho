/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 572420 - Tycho-Surefire should be executable for eclipse-plugin package type
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.osgi.framework.Version;

public class DefaultEquinoxInstallationDescription implements EquinoxInstallationDescription {
    public static final String ANY_QUALIFIER = "qualifier";
    public static final Version EQUINOX_VERSION_3_3_0 = Version.parseVersion("3.3.0");

    public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    private static final Map<String, BundleStartLevel> DEFAULT_START_LEVEL = new HashMap<>();

    static {
        setDefaultStartLevel("org.eclipse.equinox.common", 2);
        setDefaultStartLevel("org.eclipse.core.runtime", 4);
        setDefaultStartLevel("org.eclipse.equinox.simpleconfigurator", 1);
        setDefaultStartLevel("org.eclipse.osgi", -1);
        setDefaultStartLevel("org.apache.felix.scr", 1);
    }

    private static void setDefaultStartLevel(String id, int level) {
        DEFAULT_START_LEVEL.put(id, new BundleStartLevel(id, level, true));
    }

    protected final Map<String, SortedMap<Version, BundleReference>> bundles = new HashMap<>();

    private final Map<String, BundleStartLevel> startLevel = new HashMap<>(DEFAULT_START_LEVEL);

    private BundleStartLevel defaultBundleStartLevel = null;

    private final List<File> frameworkExtensions = new ArrayList<>();

    private final Set<String> bundlesToExplode = new HashSet<>();

    private final Map<String, String> platformProperties = new HashMap<>();

    private final Map<String, String> devEntries = new HashMap<>();

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
    public BundleReference getBundle(String key, String version) {
        SortedMap<Version, BundleReference> map = bundles.get(key);
        if (map == null || map.isEmpty()) {
            return null;
        }

        Version parsedVersion;
        if (version == null) {
            parsedVersion = Version.emptyVersion;
        } else {
            parsedVersion = Version.parseVersion(version);
        }
        if (Version.emptyVersion.equals(Version.parseVersion(version))) {
            return map.get(map.firstKey());
        }
        String qualifier = parsedVersion.getQualifier();
        if (qualifier == null || qualifier.isBlank() || DependencyArtifacts.ANY_QUALIFIER.equals(qualifier)) {
            // latest qualifier
            for (Entry<Version, BundleReference> entry : map.entrySet()) {
                if (baseVersionEquals(parsedVersion, entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return map.get(parsedVersion);
    }

    private static boolean baseVersionEquals(Version v1, Version v2) {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro();
    }

    @Override
    public Collection<BundleReference> getBundles() {
        return bundles.values().stream().flatMap(map -> map.values().stream()).toList();
    }

    @Override
    public BundleReference getSystemBundle() {
        return getBundle(EquinoxContainer.NAME, null);
    }

    @Override
    public void addBundle(BundleReference reference) {
        bundles.computeIfAbsent(reference.getId(), k -> new TreeMap<>()).put(new Version(reference.getVersion()),
                reference);
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
            devEntries.merge(id, entries, (s1, s2) -> s1 + "," + s2);
        }
    }

    @Override
    public Map<String, String> getDevEntries() {
        return devEntries;
    }
}
