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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
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
import java.util.WeakHashMap;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.osgi.framework.Version;

// TODO 364134 rename this class
public class DefaultTargetPlatform implements DependencyArtifacts {
    private static final Version VERSION_0_0_0 = new Version("0.0.0");

    /**
     * ArtifactKey cache used to correlate equal instances to reduce memory usage
     */
    private static final WeakHashMap<ArtifactKey, ArtifactKey> KEY_CACHE = new WeakHashMap<ArtifactKey, ArtifactKey>();

    /**
     * ArtifactDescriptor cache used to correlate equal instances to reduce memory usage
     */
    private static final WeakHashMap<ArtifactDescriptor, ArtifactDescriptor> ARTIFACT_CACHE = new WeakHashMap<ArtifactDescriptor, ArtifactDescriptor>();

    /**
     * 'this' project, i.e. the project the dependencies were resolved for. can be null.
     */
    protected final ReactorProject project;

    protected final Map<ArtifactKey, ArtifactDescriptor> artifacts = new LinkedHashMap<ArtifactKey, ArtifactDescriptor>();

    protected final Map<File, Map<String, ArtifactDescriptor>> locations = new LinkedHashMap<File, Map<String, ArtifactDescriptor>>();

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    protected final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<Object>();

    public DefaultTargetPlatform() {
        this(null);
    }

    public DefaultTargetPlatform(ReactorProject project) {
        this.project = project;
    }

    public List<ArtifactDescriptor> getArtifacts(String type) {
        ArrayList<ArtifactDescriptor> result = new ArrayList<ArtifactDescriptor>();
        for (Map.Entry<ArtifactKey, ArtifactDescriptor> entry : artifacts.entrySet()) {
            if (type.equals(entry.getKey().getType())) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    public List<ArtifactDescriptor> getArtifacts() {
        return new ArrayList<ArtifactDescriptor>(artifacts.values());
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

        ArtifactKey key = normalizeKey(artifact.getKey());

        File location;
        try {
            location = artifact.getLocation().getCanonicalFile();
        } catch (IOException e) {
            // not sure what good this will do to the caller
            location = artifact.getLocation().getAbsoluteFile();
        }

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

            units = new LinkedHashSet<Object>(original.getInstallableUnits());
            units.addAll(artifact.getInstallableUnits());
        } else {
            units = artifact.getInstallableUnits();
        }

        // reuse artifact keys to reduce memory usage
        ArtifactKey cachedKey = KEY_CACHE.get(key);
        if (cachedKey != null) {
            key = cachedKey;
        } else {
            KEY_CACHE.put(key, key);
        }

        if (units != null) {
            units = Collections.unmodifiableSet(units);
        }

        // recreate artifact descriptor to use normalized location, key and units 
        artifact = new DefaultArtifactDescriptor(key, location, artifact.getMavenProject(), artifact.getClassifier(),
                units);

        // reuse cached artifact descriptor instance to reduce memory usage
        ArtifactDescriptor cachedArtifact = ARTIFACT_CACHE.get(artifact);
        if (cachedArtifact != null) {
            artifact = cachedArtifact;
        } else {
            ARTIFACT_CACHE.put(artifact, artifact);
        }

        artifacts.put(artifact.getKey(), artifact);

        Map<String, ArtifactDescriptor> classified = locations.get(location);
        if (classified == null) {
            classified = new LinkedHashMap<String, ArtifactDescriptor>();
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

    protected ArtifactKey normalizeKey(ArtifactKey key) {
        // normalize eclipse-test-plugin... after all, a bundle is a bundle.
        if (org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(key.getType())) {
            key = new DefaultArtifactKey(org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, key.getId(),
                    key.getVersion());
        }
        return key;
    }

    private static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

    /**
     * @deprecated get rid of me, I am not used for anything
     */
    public void addSite(File location) {
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
        SortedMap<Version, ArtifactDescriptor> relevantArtifacts = new TreeMap<Version, ArtifactDescriptor>(
                new Comparator<Version>() {
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

        if (qualifier == null || "".equals(qualifier) || ANY_QUALIFIER.equals(qualifier)) {
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
        try {
            location = location.getCanonicalFile();
            return locations.get(location);
        } catch (IOException e) {
            return null;
        }
    }

    public ArtifactDescriptor getArtifact(ArtifactKey key) {
        return artifacts.get(normalizeKey(key));
    }

    public void removeAll(String type, String id) {
        Iterator<Entry<ArtifactKey, ArtifactDescriptor>> iter = artifacts.entrySet().iterator();
        while (iter.hasNext()) {
            ArtifactKey key = iter.next().getKey();
            if (key.getType().equals(type) && key.getId().equals(id)) {
                iter.remove();
            }
        }
    }

    public Set<?/* IInstallableUnit */> getNonReactorUnits() {
        return nonReactorUnits;
    }

    public Set<?/* IInstallableUnit */> getInstallableUnits() {
        Set<Object> units = new LinkedHashSet<Object>();
        for (ArtifactDescriptor artifact : artifacts.values()) {
            if (project == null || !project.equals(artifact.getMavenProject())) {
                units.addAll(artifact.getInstallableUnits());
            }
        }
        units.addAll(nonReactorUnits);
        return Collections.unmodifiableSet(units);
    }

    public void addNonReactorUnits(Set<?/* IInstallableUnit */> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
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
