/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;

@Named(ManifestComparator.TYPE)
@Singleton
public class ManifestComparator implements ContentsComparator {

    public static final String TYPE = "manifest";

    private static enum Change {
        BASELINE_ONLY, PROJECT_ONLY, DIFFERENT;
    };

    private static final Set<Name> IGNORED_KEYS = Set.of(
            // these keys are added by plexus archiver
            new Name("Archiver-Version"), //
            new Name("Created-By"), //
            new Name("Build-Jdk"), //
            new Name("Built-By"), //
            new Name("Build-Jdk-Spec"),
            // lets be friendly to bnd/maven-bundle-plugin
            new Name("Bnd-LastModified"), //
            new Name("Bundle-Developers"), //
            new Name("Tool"),
            // this is common attribute not supported by Tycho yet
            new Name("Eclipse-SourceReferences"),
            // Java-Version is added by some tooling and should be ignored
            new Name("Java-Version"));
    // TODO make it possible to disable default ignores and add custom ignore

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        TreeMap<String, ManifestDelta> result = new TreeMap<>();

        Manifest baselineManifest = new Manifest(baseline);
        Manifest projectManifest = new Manifest(reactor);

        Attributes baselineAttributes = baselineManifest.getMainAttributes();
        Attributes projectAttributes = projectManifest.getMainAttributes();

        Set<Name> names = new LinkedHashSet<>();
        addNames(baselineAttributes, names);
        addNames(projectAttributes, names);

        for (Name key : names) {
            String baselineValue = baselineAttributes.getValue(key);
            String reactorValue = projectAttributes.getValue(key);
            if (baselineValue == null) {
                addDelta(result, key, "not present in baseline version", Change.PROJECT_ONLY, reactorValue,
                        reactorValue);
                continue;
            }

            if (reactorValue == null) {
                addDelta(result, key, "present in baseline version only", Change.BASELINE_ONLY, baselineValue,
                        baselineValue);
                continue;
            }

            if (!isEquivialentHeaderValue(key.toString(), baselineValue, reactorValue)) {
                addDelta(result, key, "baseline='" + baselineValue + "' != reactor='" + reactorValue + "'",
                        Change.DIFFERENT, baselineValue, reactorValue);
            }
        }
        checkForEEChange(result);
        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : ArtifactDelta.NO_DIFFERENCE;
    }

    private void checkForEEChange(Map<String, ManifestDelta> result) {
        try {
            if (result.size() > 1) {
                @SuppressWarnings("deprecation")
                Entry<String, ManifestDelta> bree = result.entrySet().stream()
                        .filter(e -> Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.equalsIgnoreCase(e.getKey()))
                        .findFirst().orElse(null);
                ManifestDelta breeDelta;
                if (bree != null && (breeDelta = bree.getValue()).change != Change.DIFFERENT) {
                    //check if it was migrated to ee.cap...
                    Entry<String, ManifestDelta> cap = result.entrySet().stream()
                            .filter(e -> Constants.REQUIRE_CAPABILITY.equalsIgnoreCase(e.getKey())).findFirst()
                            .orElse(null);
                    if (cap != null) {
                        ManifestDelta capDelta = cap.getValue();
                        if (capDelta.change != Change.DIFFERENT) {
                            if (isEquivialentBreeCap(breeDelta.value, capDelta.value)) {
                                result.remove(cap.getKey());
                                result.remove(bree.getKey());
                            }
                            return;
                        }
                        if (breeDelta.change == Change.BASELINE_ONLY) {
                            if (isEquivialentBreeCap(breeDelta.value, capDelta.value, capDelta.changed)) {
                                result.remove(cap.getKey());
                                result.remove(bree.getKey());
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            //just in case any invalid/unkownn values we can't process, then we simply  say it is different anyways
        }
    }

    private void addDelta(TreeMap<String, ManifestDelta> result, Name key, String message, Change change, String value,
            String changed) {
        result.put(key.toString(), new ManifestDelta(message, change, value, changed));
    }

    private void addNames(Attributes attributes, Set<Name> names) {
        for (Object key : attributes.keySet()) {
            Name name = (Name) key;
            if (!isIgnoredHeaderName(name)) {
                names.add(name);
            }
        }
    }

    public static boolean isIgnoredHeaderName(String name) {
        return name != null && IGNORED_KEYS.contains(new Name(name));
    }

    public static boolean isIgnoredHeaderName(Name name) {
        return IGNORED_KEYS.contains(name);
    }

    @Override
    public boolean matches(String extension) {
        return TYPE.equalsIgnoreCase(extension);
    }

    private static final class ManifestDelta extends SimpleArtifactDelta {

        private Change change;
        private String value;
        private String changed;

        public ManifestDelta(String message, Change change, String value, String changed) {
            super(message);
            this.change = change;
            this.value = value;
            this.changed = changed;
        }

    }

    public static boolean isEquivialentBreeCap(String bree, String cap) {
        String[] parts = bree.split("-");
        String osgiee = "osgi.ee;filter:=\"(&(osgi.ee=" + parts[0] + ")(version=" + parts[1] + "))\"";
        return cap.equalsIgnoreCase(osgiee);
    }

    public static boolean isEquivialentBreeCap(String bree, String capBase, String capChange) {
        try {
            String[] parts = bree.split("-");
            String osgiee = "osgi.ee;filter:=\"(&(osgi.ee=" + parts[0] + ")(version=" + parts[1] + "))\"";
            String withoutEE = capChange.replace(osgiee, "").replace(",,", ",");
            if (withoutEE.endsWith(",")) {
                withoutEE = withoutEE.substring(0, withoutEE.length() - 1);
            }
            if (withoutEE.startsWith(",")) {
                withoutEE = withoutEE.substring(1);
            }
            return isEquivialentHeaderValue(Constants.REQUIRE_CAPABILITY, capBase, withoutEE);
        } catch (RuntimeException e) {
            //can't compare then...
        }
        return isEquivialentHeaderValue(Constants.REQUIRE_CAPABILITY, capBase, capChange);
    }

    public static boolean isEquivialentHeaderValue(String key, String base, String project) {
        if (base != null && project != null) {
            try {
                if (Constants.REQUIRE_CAPABILITY.equalsIgnoreCase(key)) {
                    Parameters baseHeader = OSGiHeader.parseHeader(base);
                    Parameters projectHeader = OSGiHeader.parseHeader(project);
                    Set<Capability> baseCapabilities = new HashSet<>(CapReqBuilder.getCapabilitiesFrom(baseHeader));
                    Set<Capability> projectCapabilities = new HashSet<>(
                            CapReqBuilder.getCapabilitiesFrom(projectHeader));
                    return baseCapabilities.equals(projectCapabilities);
                }
            } catch (RuntimeException e) {
                //can't compare then...
            }
        }
        return Objects.equals(base, project);
    }
}
