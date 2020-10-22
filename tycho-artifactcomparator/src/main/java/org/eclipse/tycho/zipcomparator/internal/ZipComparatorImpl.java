/*******************************************************************************
 * Copyright (c) 2012-2017 Sonatype Inc. and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ArtifactComparator.class, hint = ZipComparatorImpl.TYPE)
public class ZipComparatorImpl implements ArtifactComparator {

    public static final String TYPE = "zip";

    private static final Collection<String> IGNORED_PATTERNS;

    static {
        ArrayList<String> ignoredPatterns = new ArrayList<>();

        ignoredPatterns.add("meta-inf/maven/**");

        IGNORED_PATTERNS = Collections.unmodifiableList(ignoredPatterns);
    }

    @Requirement
    private Logger log;

    @Requirement
    private Map<String, ContentsComparator> comparators;

    @Override
    public CompoundArtifactDelta getDelta(File baseline, File reactor, MojoExecution execution) throws IOException {
        Map<String, ArtifactDelta> result = new LinkedHashMap<>();
        Collection<String> ignoredPatterns = new HashSet<>(IGNORED_PATTERNS);
        if (execution != null) {
            Xpp3Dom pluginConfiguration = (Xpp3Dom) execution.getPlugin().getConfiguration();
            if (pluginConfiguration != null) {
                Xpp3Dom ignoredPatternsNode = pluginConfiguration.getChild("ignoredPatterns");
                if (ignoredPatternsNode != null) {
                    for (Xpp3Dom node : ignoredPatternsNode.getChildren()) {
                        ignoredPatterns.add(node.getValue());
                    }
                }
            }
        }

        try (ZipFile jar = new ZipFile(baseline); ZipFile jar2 = new ZipFile(reactor)) {
            Map<String, ZipEntry> entries = toEntryMap(jar, ignoredPatterns);
            Map<String, ZipEntry> entries2 = toEntryMap(jar2, ignoredPatterns);

            Set<String> names = new TreeSet<>();
            names.addAll(entries.keySet());
            names.addAll(entries2.keySet());

            for (String name : names) {
                ZipEntry entry = entries.get(name);
                if (entry == null) {
                    result.put(name, new SimpleArtifactDelta("not present in baseline"));
                    continue;
                }
                ZipEntry entry2 = entries2.get(name);
                if (entry2 == null) {
                    result.put(name, new SimpleArtifactDelta("present in baseline only"));
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry); InputStream is2 = jar2.getInputStream(entry2);) {
                    ContentsComparator comparator = comparators.get(getContentType(name));
                    ArtifactDelta differences = comparator.getDelta(is, is2, execution);
                    if (differences != null) {
                        result.put(name, differences);
                        continue;
                    }
                }
            }
        }
        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : null;
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

    private static Map<String, ZipEntry> toEntryMap(ZipFile zip, Collection<String> ignoredPatterns) {
        Map<String, ZipEntry> result = new LinkedHashMap<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && !isIgnored(entry.getName(), ignoredPatterns)) {
                result.put(entry.getName(), entry);
            }
        }
        return result;
    }

    private static boolean isIgnored(String name, Collection<String> ignoredPatterns) {
        for (String pattern : ignoredPatterns) {
            if (SelectorUtils.matchPath(pattern, name, false)) {
                return true;
            }
        }
        return false;
    }
}
