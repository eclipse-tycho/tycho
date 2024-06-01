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
package org.eclipse.tycho.core.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public class ArtifactCollection {
    private static final Version VERSION_0_0_0 = new Version("0.0.0");

    protected final Map<ArtifactKey, ArtifactDescriptor> artifacts = new LinkedHashMap<>();

    protected final Map<File, Map<String, ArtifactDescriptor>> artifactsWithKnownLocation = new LinkedHashMap<>();

    public List<ArtifactDescriptor> getArtifacts(String type) {
        return getArtifacts(key -> key.getType().equals(type));
    }

    public List<ArtifactDescriptor> getArtifacts(Predicate<ArtifactKey> filter) {
        return artifacts.entrySet().stream().filter(entry -> filter.test(entry.getKey())).map(Entry::getValue).toList();
    }

    public List<ArtifactDescriptor> getArtifacts() {
        return new ArrayList<>(artifacts.values());
    }

    public void addArtifactFile(ArtifactKey key, File location, Collection<IInstallableUnit> installableUnits) {
        addArtifact(new DefaultArtifactDescriptor(key, location, null, null, installableUnits));
    }

    public void addArtifactFile(ArtifactKey key, Supplier<File> location,
            Collection<IInstallableUnit> installableUnits) {
        addArtifact(new DefaultArtifactDescriptor(key, whatever -> location.get(), null, null, installableUnits));
    }

    public void addArtifact(ArtifactDescriptor artifact) {
        addArtifact(artifact, false);
    }

    protected void addArtifact(final ArtifactDescriptor artifact, boolean merge) {
        if (artifact.getClass() != DefaultArtifactDescriptor.class) {
            throw new IllegalAccessError();
        }

        ArtifactKey normalizedKey = normalizeKey(artifact.getKey());

        ArtifactDescriptor original = artifacts.get(normalizedKey);

        Collection<IInstallableUnit> units;
        Collection<IInstallableUnit> artifactIUs = artifact.getInstallableUnits();
        if (original != null) {
            // can't use DefaultArtifactDescriptor.equals because artifact.location is not normalized
            if (!Objects.equals(original.getClassifier(), artifact.getClassifier())) {
                throw new IllegalStateException(
                        "Inconsistent artifact with key " + artifact.getKey() + " classifier is different");
            }
            if (!Objects.equals(original.getMavenProject(), artifact.getMavenProject())) {
                throw new IllegalStateException(
                        "Inconsistent artifact with key " + artifact.getKey() + " MavenProject is different");
            }

            // artifact equals to original
            Collection<IInstallableUnit> originalIUs = original.getInstallableUnits();
            if (unitSetCompare(artifactIUs, originalIUs)) {
                if (original instanceof DefaultArtifactDescriptor def) {
                    if (original.getLocation(false) == null) {
                        //they equal but maybe we can fetch the file form the other one because it is already resolved?
                        if (original.getMavenProject() == null) {
                            def.setMavenProject(artifact.getMavenProject());
                        }
                        File newLocation = artifact.getLocation(false);
                        if (newLocation != null) {
                            def.resolve(newLocation);
                            registerArtifactLocation(newLocation, original);
                        }
                    }
                }
                return;
            }
            if (!merge) {
                if (artifactIUs.size() != originalIUs.size()) {
                    throw new IllegalStateException("Inconsistent artifact with key " + artifact.getKey()
                            + " number of IUs differ in size, existing IUs=" + originalIUs + ", artifact IUs="
                            + artifactIUs);
                }
                if (artifactIUs.size() == 1 && originalIUs.size() == 1) {
                    IInstallableUnit artifactUnit = artifactIUs.iterator().next();
                    IInstallableUnit originalUnit = originalIUs.iterator().next();
                    throw new IllegalStateException("Inconsistent artifact with key " + artifact.getKey()
                            + " existing IU = " + originalUnit + ", artifact IU = " + artifactUnit);
                }
                throw new IllegalStateException("Inconsistent artifact with key " + artifact.getKey() + "existing IUs="
                        + originalIUs + ", artifact IUs=" + artifactIUs);
            }

            units = new LinkedHashSet<>(originalIUs);
            units.addAll(artifactIUs);
        } else {
            units = artifactIUs;
        }

        if (units != null) {
            units = Collections.unmodifiableCollection(units);
        }

        // recreate artifact descriptor to use normalized location, key and units
        File location = artifact.getLocation(false);
        ArtifactDescriptor normalizedArtifact = location != null
                ? new DefaultArtifactDescriptor(normalizedKey, location, artifact.getMavenProject(),
                        artifact.getClassifier(), units)
                : new DefaultArtifactDescriptor(normalizedKey, thisArtifact -> {
                    File resolvedLocation = artifact.getLocation(true);
                    registerArtifactLocation(resolvedLocation, thisArtifact);
                    return resolvedLocation;
                }, artifact.getMavenProject(), artifact.getClassifier(), units);

        artifacts.put(normalizedKey, normalizedArtifact);
        if (location != null) {
            registerArtifactLocation(location, normalizedArtifact);
        }
    }

    private boolean unitSetCompare(Collection<IInstallableUnit> unitsA, Collection<IInstallableUnit> unitsB) {
        if (unitsA == null) {
            unitsA = Collections.emptyList();
        }
        if (unitsB == null) {
            unitsB = Collections.emptyList();
        }
        //usual and easy case ...
        if (unitsA.size() == 1 && unitsB.size() == 1) {
            IInstallableUnit a = unitsA.iterator().next();
            IInstallableUnit b = unitsB.iterator().next();
            return Objects.equals(a, b);
        }
        //if more then we create sets and compare them ...
        return Set.copyOf(unitsB).equals(Set.copyOf(unitsA));
    }

    private void registerArtifactLocation(File location, ArtifactDescriptor normalizedArtifact) {
        Map<String, ArtifactDescriptor> classified = artifactsWithKnownLocation.computeIfAbsent(location,
                loc -> new LinkedHashMap<>());
        // TODO sanity check, no duplicate artifact classifiers at the same location
        //if (classified.containsKey(artifact.getClassifier())) {
        //    throw new IllegalStateException("Duplicate artifact classifier at location " + location);
        //}
        // sanity check, all artifacts at the same location have the same reactor project
        for (ArtifactDescriptor other : classified.values()) {
            if (!Objects.equals(normalizedArtifact.getMavenProject(), other.getMavenProject())) {
                throw new IllegalStateException("Inconsistent reactor project at location " + location + ". "
                        + normalizedArtifact.getMavenProject() + " is not the same as " + other.getMavenProject());
            }
        }
        classified.put(normalizedArtifact.getClassifier(), normalizedArtifact);
    }

    // ideally this would return a specialized type -> the type checker would then ensure that this is called wherever needed
    public static File normalizeLocation(File location) {
        // don't call getCanonicalFile here because otherwise we'll be forced to call getCanonical* everywhere
        return new File(location.getAbsoluteFile().toURI().normalize());
    }

    protected ArtifactDescriptor normalize(ArtifactDescriptor artifact) {
        return artifact;
    }

    protected ArtifactKey normalize(ArtifactKey key) {
        return key;
    }

    private ArtifactKey normalizeKey(ArtifactKey key) {
        // normalize eclipse-test-plugin... after all, a bundle is a bundle.
        // TODO ArtifactKey should never use packaging types
        if (PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(key.getType())) {
            key = new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, key.getId(), key.getVersion());
        }
        if (!(key instanceof DefaultArtifactKey)) {
            return new DefaultArtifactKey(key.getType(), key.getId(), key.getVersion());
        }
        return key;
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
        SortedMap<Version, ArtifactDescriptor> relevantArtifacts = new TreeMap<>((o1, o2) -> -o1.compareTo(o2));

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
        if (version.startsWith("(") || version.startsWith("[")) {
            VersionRange range = VersionRange.valueOf(version);
            for (Entry<Version, ArtifactDescriptor> entry : relevantArtifacts.entrySet()) {
                if (range.includes(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        Version parsedVersion = new Version(version);
        if (VERSION_0_0_0.equals(parsedVersion)) {
            return relevantArtifacts.get(relevantArtifacts.firstKey()); // latest version
        }

        String qualifier = parsedVersion.getQualifier();

        if (qualifier == null || qualifier.isBlank() || DependencyArtifacts.ANY_QUALIFIER.equals(qualifier)) {
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
            Collection<IInstallableUnit> installableUnits) {
        DefaultArtifactDescriptor artifact = new DefaultArtifactDescriptor(key, project.getBasedir(), project,
                classifier, installableUnits);
        addArtifact(artifact);
    }

    public ReactorProject getMavenProject(File location) {
        // only check artifactsWithKnownLocation as we're expected a local reactor project, location is already set
        Map<String, ArtifactDescriptor> classified = artifactsWithKnownLocation.get(normalizeLocation(location));
        if (classified != null) {
            // #addArtifact enforces all artifacts at the same location have the same reactor project 
            return classified.values().iterator().next().getMavenProject();
        }
        return null;
    }

    /**
     * This triggers fetch of all dependencies.
     * 
     * @param location
     * @return
     */
    public Map<String, ArtifactDescriptor> getArtifact(File location) {
        artifacts.values().forEach(artifact -> artifact.getLocation(true));
        return artifactsWithKnownLocation.get(normalizeLocation(location));
    }

    public ArtifactDescriptor getArtifact(ArtifactKey key) {
        return artifacts.get(normalizeKey(key));
    }

    public void removeAll(String type, String id) {
        Iterator<Entry<ArtifactKey, ArtifactDescriptor>> iter = artifacts.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ArtifactKey, ArtifactDescriptor> entry = iter.next();
            ArtifactKey key = entry.getKey();
            if (key.getType().equals(type) && key.getId().equals(id)) {
                File location = entry.getValue().getLocation().orElse(null);
                if (location != null) {
                    artifactsWithKnownLocation.remove(location);
                }
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
                sb.append(artifact.getLocation(false));
            }
            sb.append("\n");
        }
    }

}
