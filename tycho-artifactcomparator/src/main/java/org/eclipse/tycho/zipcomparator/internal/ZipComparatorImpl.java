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
 *    Mickael Istria (Red Hat Inc.) - 522531 Baseline allows to ignore files
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ArtifactComparator.class, hint = ZipComparatorImpl.TYPE)
public class ZipComparatorImpl implements ArtifactComparator {

    public static final String TYPE = "zip";

    private static final List<String> IGNORED_PATTERNS = List.of("META-INF/maven/**");

    @Requirement
    private Logger log;

    @Requirement
    private Map<String, ContentsComparator> comparators;

    @Override
    public CompoundArtifactDelta getDelta(File baseline, File reactor, ComparisonData data) throws IOException {
        Map<String, ArtifactDelta> result = new LinkedHashMap<>();
        Collection<String> ignoredPatterns = new HashSet<>(IGNORED_PATTERNS);
        ignoredPatterns.addAll(data.ignoredPattern());
        MatchPatterns ignored = MatchPatterns.from(ignoredPatterns);

        try (ZipFile baselineJar = new ZipFile(baseline); ZipFile reactorJar = new ZipFile(reactor)) {
            Map<String, ZipEntry> baselineEntries = toEntryMap(baselineJar, ignored);
            Map<String, ZipEntry> reachtorEntries = toEntryMap(reactorJar, ignored);

            Set<String> names = new TreeSet<>();
            names.addAll(baselineEntries.keySet());
            names.addAll(reachtorEntries.keySet());

            for (String name : names) {
                ArtifactDelta delta = getDelta(name, baselineEntries, reachtorEntries, baselineJar, reactorJar, data);
                if (delta != null) {
                    result.put(name, delta);
                }
            }
        }
        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : null;
    }

    private ArtifactDelta getDelta(String name, Map<String, ZipEntry> baseline, Map<String, ZipEntry> reactor,
            ZipFile baselineJar, ZipFile reactorJar, ComparisonData data) throws IOException {
        ZipEntry baselineEntry = baseline.get(name);
        if (baselineEntry == null) {
            return new SimpleArtifactDelta("not present in baseline");
        }
        ZipEntry reactorEntry = reactor.get(name);
        if (reactorEntry == null) {
            return new SimpleArtifactDelta("present in baseline only");
        }

        try (InputStream is = baselineJar.getInputStream(baselineEntry);
                InputStream is2 = reactorJar.getInputStream(reactorEntry);) {
            ContentsComparator comparator = comparators.get(getContentType(name));
            return comparator.getDelta(is, is2, data);
        }
    }

    private String getContentType(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".class")) {
            return ClassfileComparator.TYPE;
        }
        if (name.endsWith(".jar") || name.endsWith(".zip")) {
            return NestedZipComparator.TYPE;
        }
        if (name.endsWith(".properties") || name.endsWith(".mappings")) {
            // .mapping comes from org.eclipse.equinox.p2.internal.repository.comparator.JarComparator
            return PropertiesComparator.TYPE;
        }
        if ("meta-inf/manifest.mf".equals(name)) {
            return ManifestComparator.TYPE;
        }
        return DefaultContentsComparator.TYPE;
    }

    private static Map<String, ZipEntry> toEntryMap(ZipFile zip, MatchPatterns ignored) {
        Map<String, ZipEntry> result = new LinkedHashMap<>(zip.size());
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && !ignored.matches(entry.getName(), false)) {
                result.put(entry.getName(), entry);
            }
        }
        return result;
    }
}
