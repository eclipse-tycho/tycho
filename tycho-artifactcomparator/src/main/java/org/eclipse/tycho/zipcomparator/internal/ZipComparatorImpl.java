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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Named(ZipComparatorImpl.TYPE)
@Singleton
public class ZipComparatorImpl implements ArtifactComparator {

    public static final String TYPE = "zip";

    private static final List<String> IGNORED_PATTERNS = List.of("META-INF/maven/**");

    @Inject
    private Logger log;

    @Inject
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
        return !result.isEmpty() ? new ZipArtifactDelta(result, baseline, reactor) : null;
    }

    private static final class ZipArtifactDelta extends CompoundArtifactDelta {

        private File baseline;
        private File reactor;

        public ZipArtifactDelta(Map<String, ? extends ArtifactDelta> members, File baseline, File reactor) {
            super("different", members);
            this.baseline = baseline;
            this.reactor = reactor;
        }

        @Override
        public void writeDetails(File basedir) throws IOException {
            basedir.mkdirs();
            super.writeDetails(basedir);
            if (baseline.isFile()) {
                Files.copy(baseline.toPath(), basedir.toPath().resolve("baseline-" + baseline.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            if (reactor.isFile()) {
                Files.copy(reactor.toPath(), basedir.toPath().resolve("build-" + reactor.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

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
            byte[] baselineBytes = baseline.readAllBytes();
            byte[] reactorBytes = reactor.readAllBytes();
            if (Arrays.equals(baselineBytes, reactorBytes)) {
                return ArtifactDelta.NO_DIFFERENCE;
            }
            ContentsComparator comparator = getContentsComparator(name);
            if (comparator != null && baselineBytes.length < ContentsComparator.THRESHOLD
                    && reactorBytes.length < ContentsComparator.THRESHOLD) {
                try {
                    return comparator.getDelta(new ComparatorInputStream(baselineBytes),
                            new ComparatorInputStream(reactorBytes), data);
                } catch (IOException e) {
                    log.debug("comparing entry " + name + " (baseline = " + baselineJar.getName() + ", reactor="
                            + reactorJar.getName() + ") using " + comparator.getClass().getName() + " failed with: " + e
                            + ", using direct byte compare", e);
                }
            }
            return ArtifactDelta.DEFAULT;
        }
    }

    private ContentsComparator getContentsComparator(String name) {
        String extension = FilenameUtils.getExtension(name).toLowerCase();
        ContentsComparator comparator = comparators.get(extension);
        if (comparator != null) {
            return comparator;
        }
        if (name.equalsIgnoreCase("meta-inf/manifest.mf")) {
            return comparators.get(ManifestComparator.TYPE);
        }
        return comparators.values().stream() //
                .filter(c -> c.matches(name) || c.matches(extension)) //
                .findFirst().orElseGet(() -> comparators.get(DefaultContentsComparator.TYPE));
    }

    private static Map<String, ZipEntry> toEntryMap(ZipFile zip, MatchPatterns ignored) {
        return zip.stream() //
                .filter(e -> !e.isDirectory() && !ignored.matches(e.getName(), false))
                .collect(Collectors.toMap(e -> e.getName(), Function.identity()));
    }
}
