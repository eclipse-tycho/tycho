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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class P2ResolutionResult {

    public static class Entry {
        private final String type;

        private final String id;

        private final String version;

        private final File location;

        private Set<Object> installableUnits;

        private final String classifier;

        public Entry(String type, String id, String version, File location, String classifier) {
            this.type = type;
            this.id = id;
            this.version = version;
            this.location = location;
            this.classifier = classifier;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public File getLocation() {
            return location;
        }

        public Set<Object> getInstallableUnits() {
            return installableUnits;
        }

        void addInstallableUnit(Object installableUnit) {
            if (installableUnits == null) {
                installableUnits = new LinkedHashSet<Object>();
            }
            installableUnits.add(installableUnit);
        }

        public String getClassifier() {
            return classifier;
        }
    }

    private final Map<List<String>, Entry> entries = new HashMap<List<String>, Entry>();

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    private final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<Object>();

    /**
     * @param type
     *            is one of P2Resolver.TYPE_* constants
     * @param id
     *            is Eclipse/OSGi artifact id
     * @param version
     *            is Eclipse/OSGi artifact version
     */
    public void addArtifact(String type, String id, String version, File location, String classifier,
            Object installableUnit) {
        // {type,id,version} is unique and not null
        // {location,classifier} is unique but can be null for metadata-only results

        List<String> key = newKey(type, id, version);

        Entry entry = entries.get(key);

        if (entry == null) {
            entry = new Entry(type, id, version, location, classifier);
            entries.put(key, entry);
        } else {
            if (!eq(entry.getLocation(), location) || !eq(entry.getClassifier(), classifier)) {
                throw new IllegalArgumentException("Conflicting results for artifact at location " + location);
            }
        }

        entry.addInstallableUnit(installableUnit);
    }

    private List<String> newKey(String type, String id, String version) {
        ArrayList<String> key = new ArrayList<String>();
        key.add(type);
        key.add(id);
        key.add(version);
        return key;
    }

    public Collection<Entry> getArtifacts() {
        return entries.values();
    }

    public Set<?> getNonReactorUnits() {
        return nonReactorUnits;
    }

    public void addNonReactorUnit(Object/* IInstallableUnit */installableUnit) {
        this.nonReactorUnits.add(installableUnit);
    }

    public void addNonReactorUnits(Set<?/* IInstallableUnit */> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
    }

    static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

}
