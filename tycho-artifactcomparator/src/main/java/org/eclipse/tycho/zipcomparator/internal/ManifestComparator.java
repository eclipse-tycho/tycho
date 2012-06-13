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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ContentsComparator.class, hint = ManifestComparator.TYPE)
public class ManifestComparator implements ContentsComparator {

    public static final String TYPE = "manifest";

    private static final Collection<Name> IGNORED_KEYS;

    static {
        ArrayList<Name> ignoredKeys = new ArrayList<Name>();

        // these keys are added by plexus archiver
        ignoredKeys.add(new Name("Archiver-Version"));
        ignoredKeys.add(new Name("Created-By"));
        ignoredKeys.add(new Name("Build-Jdk"));
        ignoredKeys.add(new Name("Built-By"));

        // lets be friendly to bnd/maven-bundle-plugin
        ignoredKeys.add(new Name("Bnd-LastModified"));
        ignoredKeys.add(new Name("Tool"));

        // this is common attribute not supported by Tycho yet
        ignoredKeys.add(new Name("Eclipse-SourceReferences"));

        // TODO make to possible to disable default ignores and add custom ignore

        IGNORED_KEYS = Collections.unmodifiableCollection(ignoredKeys);
    }

    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor) throws IOException {
        TreeMap<String, ArtifactDelta> result = new TreeMap<String, ArtifactDelta>();

        Manifest manifest = new Manifest(baseline);
        Manifest manifest2 = new Manifest(reactor);

        Attributes attributes = manifest.getMainAttributes();
        Attributes attributes2 = manifest2.getMainAttributes();

        Set<Name> names = new LinkedHashSet<Name>();
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

            if (value != null ? !value.equals(value2) : value2 != null) {
                addDelta(result, key, "baseline='" + value + "' != reactor='" + value2 + "'");
            }
        }

        return !result.isEmpty() ? new CompoundArtifactDelta("different", result) : null;
    }

    protected void addDelta(TreeMap<String, ArtifactDelta> result, Name key, String message) {
        result.put(key.toString(), new SimpleArtifactDelta(message));
    }

    protected Set<Name> getNames(Attributes attributes) {
        Set<Name> result = new LinkedHashSet<Name>();
        for (Object key : attributes.keySet()) {
            Name name = (Name) key;
            if (!IGNORED_KEYS.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }
}
