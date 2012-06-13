/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.SelectorUtils;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ArtifactComparator.class, hint = ZipComparatorImpl.TYPE)
public class ZipComparatorImpl implements ArtifactComparator {

    public static final String TYPE = "zip";

    private static final Collection<String> IGNORED_PATTERNS;

    static {
        ArrayList<String> ignoredPatterns = new ArrayList<String>();

        ignoredPatterns.add("meta-inf/maven/**");

        IGNORED_PATTERNS = Collections.unmodifiableList(ignoredPatterns);
    }

    @Requirement
    private Map<String, ContentsComparator> comparators;

    public ArtifactDelta getDelta(File baseline, File reactor) throws IOException {
        Map<String, ArtifactDelta> result = new LinkedHashMap<String, ArtifactDelta>();

        ZipFile jar = new ZipFile(baseline);
        try {
            ZipFile jar2 = new ZipFile(reactor);
            try {
                Map<String, ZipEntry> entries = toEntryMap(jar);
                Map<String, ZipEntry> entries2 = toEntryMap(jar2);

                Set<String> names = new TreeSet<String>();
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

                    InputStream is = jar.getInputStream(entry);
                    try {
                        InputStream is2 = jar2.getInputStream(entry2);
                        try {
                            ContentsComparator comparator = comparators.get(getContentType(name));
                            ArtifactDelta differences = comparator.getDelta(is, is2);
                            if (differences != null) {
                                result.put(name, differences);
                                continue;
                            }
                        } finally {
                            IOUtil.close(is2);
                        }
                    } finally {
                        IOUtil.close(is);
                    }
                }
            } finally {
                try {
                    jar2.close();
                } catch (IOException e) {
                    // too bad
                }
            }
        } finally {
            try {
                jar.close();
            } catch (IOException e) {
                // ouch
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

    private Map<String, ZipEntry> toEntryMap(ZipFile zip) {
        Map<String, ZipEntry> result = new LinkedHashMap<String, ZipEntry>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && !isIgnored(entry.getName())) {
                result.put(entry.getName(), entry);
            }
        }
        return result;
    }

    private boolean isIgnored(String name) {
        for (String pattern : IGNORED_PATTERNS) {
            if (SelectorUtils.matchPath(pattern, name, false)) {
                return true;
            }
        }
        return false;
    }
}
