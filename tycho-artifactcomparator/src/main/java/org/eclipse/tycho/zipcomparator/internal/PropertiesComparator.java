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
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ContentsComparator.class, hint = PropertiesComparator.TYPE)
public class PropertiesComparator implements ContentsComparator {
    public static final String TYPE = "properties";

    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor) throws IOException {
        TreeMap<String, ArtifactDelta> result = new TreeMap<String, ArtifactDelta>();

        Properties props = new Properties();
        props.load(baseline);
        Properties props2 = new Properties();
        props2.load(reactor);

        Set<String> names = new LinkedHashSet<String>();
        addAll(names, props);
        addAll(names, props2);

        for (String name : names) {
            String value = props.getProperty(name);
            if (value == null) {
                result.put(name, new SimpleArtifactDelta("not present in baseline version"));
                continue;
            }

            String value2 = props2.getProperty(name);
            if (value2 == null) {
                result.put(name, new SimpleArtifactDelta("present in baseline version only"));
                continue;
            }

            if (value != null ? !value.equals(value2) : value2 != null) {
                result.put(name, new SimpleArtifactDelta("baseline='" + value + "' != reactor='" + value2 + "'"));
            }
        }

        return !result.isEmpty() ? new CompoundArtifactDelta("properties files differ", result) : null;
    }

    private void addAll(Set<String> names, Properties props) {
        for (Object key : props.keySet()) {
            Object value = props.get(key);
            if (key instanceof String && value instanceof String) {
                names.add((String) key);
            }
        }
    }
}
