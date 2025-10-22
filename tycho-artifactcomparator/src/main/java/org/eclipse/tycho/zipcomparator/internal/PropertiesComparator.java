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
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;

@Named(PropertiesComparator.TYPE)
@Singleton
public class PropertiesComparator implements ContentsComparator {
    public static final String TYPE = "properties";

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        TreeMap<String, ArtifactDelta> result = new TreeMap<>();

        Properties props = new Properties();
        props.load(baseline);
        Properties props2 = new Properties();
        props2.load(reactor);

        Set<String> names = new LinkedHashSet<>();
        addAll(names, props);
        addAll(names, props2);

        for (String name : names) {
            String value = props.getProperty(name);
            if (value == null) {
                result.put(name, ArtifactDelta.MISSING_FROM_BASELINE);
                continue;
            }

            String value2 = props2.getProperty(name);
            if (value2 == null) {
                result.put(name, ArtifactDelta.BASELINE_ONLY);
                continue;
            }

            if (!value.equals(value2)) {
                result.put(name, new SimpleArtifactDelta("baseline='" + value + "' != reactor='" + value2 + "'"));
            }
        }

        return !result.isEmpty() ? new CompoundArtifactDelta("properties files differ", result) : null;
    }

    private void addAll(Set<String> names, Properties props) {
        for (Entry<Object, Object> propEntry : props.entrySet()) {
            Object key = propEntry.getKey();
            Object value = propEntry.getValue();
            if (key instanceof String keyString && value instanceof String) {
                names.add(keyString);
            }
        }
    }

    @Override
    public boolean matches(String extension) {
        return TYPE.equalsIgnoreCase(extension) || "bnd".equalsIgnoreCase(extension)
        // .mapping comes from org.eclipse.equinox.p2.internal.repository.comparator.JarComparator
                || "mappings".equalsIgnoreCase(extension);
    }
}
