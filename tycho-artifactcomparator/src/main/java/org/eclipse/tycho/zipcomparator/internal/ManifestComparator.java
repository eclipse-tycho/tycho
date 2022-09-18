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
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ContentsComparator.class, hint = ManifestComparator.TYPE)
public class ManifestComparator implements ContentsComparator {

    public static final String TYPE = "manifest";

    private static final Set<Name> IGNORED_KEYS = Set.of(
            // these keys are added by plexus archiver
            new Name("Archiver-Version"), //
            new Name("Created-By"), //
            new Name("Build-Jdk"), //
            new Name("Built-By"), //
            new Name("Build-Jdk-Spec"),
            // lets be friendly to bnd/maven-bundle-plugin
            new Name("Bnd-LastModified"), //
            new Name("Tool"),
            // this is common attribute not supported by Tycho yet
            new Name("Eclipse-SourceReferences"));
    // TODO make it possible to disable default ignores and add custom ignore

    @Override
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor, ComparisonData data) throws IOException {
        TreeMap<String, ArtifactDelta> result = new TreeMap<>();

        Manifest manifest = new Manifest(baseline);
        Manifest manifest2 = new Manifest(reactor);

        Attributes attributes = manifest.getMainAttributes();
        Attributes attributes2 = manifest2.getMainAttributes();

        Set<Name> names = new LinkedHashSet<>();
        names.addAll(getNames(attributes));
        names.addAll(getNames(attributes2));

        for (Name key : names) {
            String value = attributes.getValue(key);
            if (value == null) {
                addDelta(result, key, "not present in baseline version");
                continue;
            }

            String value2 = attributes2.getValue(key);
            if (value2 == null) {
                addDelta(result, key, "present in baseline version only");
                continue;
            }

            if (!value.equals(value2)) {
                addDelta(result, key, "baseline='" + value + "' != reactor='" + value2 + "'");
            }
        }

        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : null;
    }

    private void addDelta(TreeMap<String, ArtifactDelta> result, Name key, String message) {
        result.put(key.toString(), new SimpleArtifactDelta(message));
    }

    protected Set<Name> getNames(Attributes attributes) {
        Set<Name> result = new LinkedHashSet<>();
        for (Object key : attributes.keySet()) {
            Name name = (Name) key;
            if (!IGNORED_KEYS.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }
}
