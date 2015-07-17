/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.osgi.framework.Version;

public class ArtifactCollection {
    private static final Version VERSION_0_0_0 = new Version("0.0.0");

    protected final Map<ArtifactKey, ArtifactDescriptor> artifacts = new LinkedHashMap<>();

    protected final Map<File, Map<String, ArtifactDescriptor>> locations = new LinkedHashMap<>();

    public List<ArtifactDescriptor> getArtifacts(String type) {
        ArrayList<ArtifactDescriptor> result = new ArrayList<>();
        for (Map.Entry<ArtifactKey, ArtifactDescriptor> entry : artifacts.entrySet()) {
            if (type.equals(entry.getKey().getType())) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    public List<ArtifactDescriptor> getArtifacts() {
        return new ArrayList<>(artifacts.values());
    }

    public void addArtifactFile(ArtifactKey key, File location, Set<Object> installableUnits) {
        addArtifact(new DefaultArtifactDescriptor(key, location, null, null, installableUnits));
    }

    public void addArtifact(ArtifactDescriptor artifact) {
        addArtifact(artifact, false);
    }

    protected void addArtifact(ArtifactDescriptor artifact, boolean merge) {
        if (artifact.getClass() != DefaultArtifactDescriptor.class) {
            throw new IllegalAccessError();
        }

        ArtifactKey key = normalizePluginType(artifact.getKey());

        File location = normalizeLocation(artifact.getLocation());

        ArtifactDescriptor original = artifacts.get(key);

        Set<Object> units = null;

        if (original != null) {
            // can't use DefaultArtifactDescriptor.equals because artifact.location is not normalized
            if (!eq(original.getLocation(), location) || !eq(original.getClassifier(), artifact.getClassifier())
                    || !eq(original.getMavenProject(), artifact.getMavenProject())) {
                // TODO better error message
                throw new IllegalStateException("Inconsistent artifact with key " + artifact.getKey());
            }

            // artifact equals to original
            if (eq(original.getInstallableUnits(), artifact.getInstallableUnits())) {
                return;
            }

            if (!merge) {
                // TODO better error message
                throw new IllegalStateException("Inconsistent artifact with key " + artifact.getKey());
            }

            units = new LinkedHashSet<>(original.getInstallableUnits());
            units.addAll(artifact.getInstallableUnits());
        } else {
            units = artifact.getInstallableUnits();
        }

        // reuse artifact keys to reduce memory usage
        key = normalize(key);

        if (units != null) {
            units = Collections.unmodifiableSet(units);
        }

        // recreate artifact descriptor to use normalized location, key and units 
        artifact = new DefaultArtifactDescriptor(key, location, artifact.getMavenProject(), artifact.getClassifier(),
                units);

        // for external artifacts, reuse cached artifact descriptor instance to reduce memory usage
        // do not cache reactor project artifact descriptors because their IUs can change without changing (id,version)
        if (artifact.getMavenProject() == null) {
            artifact = normalize(artifact);
        }

        artifacts.put(artifact.getKey(), artifact);

        Map<String, ArtifactDescriptor> classified = locations.get(location);
        if (classified == null) {
            classified = new LinkedHashMap<>();
            locations.put(location, classified);
        }

        // TODO sanity check, no duplicate artifact classifiers at the same location
        //if (classified.containsKey(artifact.getClassifier())) {
        //    throw new IllegalStateException("Duplicate artifact classifier at location " + location);
        //}

        // sanity check, all artifacts at the same location have the same reactor project
        for (ArtifactDescriptor other : classified.values()) {
            if (!eq(artifact.getMavenProject(), other.getMavenProject())) {
                throw new IllegalStateException("Inconsistent reactor project at location " + location + ". "
                        + artifact.getMavenProject() + " is not the same as " + other.getMavenProject());
            }
        }

        classified.put(artifact.getClassifier(), artifact);
    }

    // ideally this would return a specialized type -> the type checker would then ensure that this is called wherever needed
    private static File normalizeLocation(File location) {
        // don't call getCanonicalFile here because otherwise we'll be forced to call getCanonical* everywhere
        return new File(location.getAbsoluteFile().toURI().normalize());
    }

    protected ArtifactDescriptor normalize(ArtifactDescriptor artifact) {
        return artifact;
    }

    protected ArtifactKey normalize(ArtifactKey key) {
        return key;
    }

    protected ArtifactKey normalizePluginType(ArtifactKey key) {
        // normalize eclipse-test-plugin... after all, a bundle is a bundle.
        // TODO ArtifactKey should never use packaging types
        if (PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(key.getType())) {
            key = new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, key.getId(), key.getVersion());
        }
        return key;
    }

    private static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

    public void dump() {
        for (Map.Entry<ArtifactKey, ArtifactDescriptor> entry : artifacts.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }

    public boolean isEmpty() {
        return artifacts.isEmpty();
    }

    public ArtifactDescriptor getArtifact(String type, String id, String version) {
        if (type == null || id == null) {
            // TODO should we throw something instead?
            return null;
        }

        // features with matching id, sorted by version, highest version first
        SortedMap<Version, ArtifactDescriptor> relevantArtifacts = new TreeMap<>(
                new Comparator<Version>() {
                    @Override
                    public int compare(Version o1, Version o2) {
                        return -o1.compareTo(o2);
                    };
                });

        for (Map.Entry<ArtifactKey, ArtifactDescriptor> entry : this.artifacts.entrySet()) {
            ArtifactKey key = entry.getKey();
            if (type.equals(key.getType()) && id.equals(key.getId())) {
                relevantArtifacts.put(Version.parseVersion(key.getVersion()), entry.getValue());
            }
        }

        if (relevantArtifacts.isEmpty()) {
            return null;
        }

        if (version == null) {
            return relevantArtifacts.get(relevantArtifacts.firstKey()); // latest version
        }

        Version parsedVersion = new Version(version);
        if (VERSION_0_0_0.equals(parsedVersion)) {
            return relevantArtifacts.get(relevantArtifacts.firstKey()); // latest version
        }

        String qualifier = parsedVersion.getQualifier();

        if (qualifier == null || "".equals(qualifier) || DependencyArtifacts.ANY_QUALIFIER.equals(qualifier)) {
            // latest qualifier
            for (Map.Entry<Version, ArtifactDescriptor> entry : relevantArtifacts.entrySet()) {
                if (baseVersionEquals(parsedVersion, entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        // perfect match or nothing
        return relevantArtifacts.get(parsedVersion);
    }

    private static boolean baseVersionEquals(Version v1, Version v2) {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro();
    }

    public void addReactorArtifact(ArtifactKey key, ReactorProject project, String classifier,
            Set<Object> installableUnits) {
        DefaultArtifactDescriptor artifact = new DefaultArtifactDescriptor(key, project.getBasedir(), project,
                classifier, installableUnits);
        addArtifact(artifact);
    }

    public ReactorProject getMavenProject(File location) {
        Map<String, ArtifactDescriptor> classified = getArtifact(location);
        if (classified != null) {
            // #addArtifact enforces all artifacts at the same location have the same reactor project 
            return classified.values().iterator().next().getMavenProject();
        }
        return null;
    }

    public Map<String, ArtifactDescriptor> getArtifact(File location) {
        return locations.get(normalizeLocation(location));
    }

    public ArtifactDescriptor getArtifact(ArtifactKey key) {
        return artifacts.get(normalizePluginType(key));
    }

    public void removeAll(String type, String id) {
        Iterator<Entry<ArtifactKey, ArtifactDescriptor>> iter = artifacts.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ArtifactKey, ArtifactDescriptor> entry = iter.next();
            ArtifactKey key = entry.getKey();
            if (key.getType().equals(type) && key.getId().equals(id)) {
                locations.remove(entry.getValue().getLocation());
                iter.remove();
            }
        }
    }

    public void toDebugString(StringBuilder sb, String linePrefix) {
        for (ArtifactDescriptor artifact : artifacts.values()) {
            sb.append(linePrefix);
            sb.append(artifact.getKey().toString());
            sb.append(": ");
            ReactorProject project = artifact.getMavenProject();
            if (project != null) {
                sb.append(project.toString());
            } else {
                sb.append(artifact.getLocation());
            }
            sb.append("\n");
        }
    }

}
