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
import java.util.Arrays;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Component(role = ArtifactComparator.class, hint = ZipComparatorImpl.TYPE)
public class ZipComparatorImpl implements ArtifactComparator {

    public static final String TYPE = "zip";

    private static final List<String> IGNORED_PATTERNS = List.of("META-INF/maven/**");

    @Requirement
    private Logger log;

    @Requirement
    private Map<String, ContentsComparator> comparators;

    @Override
    public ArtifactDelta getDelta(File baseline, File reactor, ComparisonData data) throws IOException {
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
        } catch (IOException e) {
            log.debug("Comparing baseline=" + baseline + " with reactor=" + reactor + " failed: " + e
                    + " using direct byte compare!", e);
            //this can happen if we compare files that seem zip files but are actually not, for example an embedded jar can be an (empty) dummy file... in this case we should fall back to dumb byte compare (better than fail...)
            if (FileUtils.contentEquals(baseline, reactor)) {
                return null;
            }
            return ArtifactDelta.DEFAULT;
        }
        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : null;
    }

    private ArtifactDelta getDelta(String name, Map<String, ZipEntry> baselineMap, Map<String, ZipEntry> reactorMap,
            ZipFile baselineJar, ZipFile reactorJar, ComparisonData data) throws IOException {
        ZipEntry baselineEntry = baselineMap.get(name);
        if (baselineEntry == null) {
            return ArtifactDelta.MISSING_FROM_BASELINE;
        }
        ZipEntry reactorEntry = reactorMap.get(name);
        if (reactorEntry == null) {
            return ArtifactDelta.BASELINE_ONLY;
        }

        try (InputStream baseline = baselineJar.getInputStream(baselineEntry);
                InputStream reactor = reactorJar.getInputStream(reactorEntry);) {
            byte[] baselineBytes = IOUtils.toByteArray(baseline);
            byte[] reactorBytes = IOUtils.toByteArray(reactor);
            ArtifactDelta direct = compareDirect(baselineBytes, reactorBytes);
            if (direct == null) {
                //perfectly equal!
                return null;
            }
            ContentsComparator comparator = comparators.get(getContentType(name));
            if (comparator != null) {
                try {
                    return comparator.getDelta(new ComparatorInputStream(baselineBytes),
                            new ComparatorInputStream(reactorBytes), data);
                } catch (IOException e) {
                    log.debug("comparing entry " + name + " (baseline = " + baselineJar.getName() + ", reactor="
                            + reactorJar.getName() + ") using " + comparator.getClass().getName() + " failed with: " + e
                            + ", using direct byte compare...", e);
                }
            }
            return direct;
        }
    }

    private static ArtifactDelta compareDirect(byte[] baselineBytes, byte[] reactorBytes) {
        if (Arrays.equals(baselineBytes, reactorBytes)) {
            return ArtifactDelta.NO_DIFFERENCE;
        } else {
            return ArtifactDelta.DEFAULT;
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
        if (name.endsWith(".xml")) {
            return XmlComparator.XML;
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
