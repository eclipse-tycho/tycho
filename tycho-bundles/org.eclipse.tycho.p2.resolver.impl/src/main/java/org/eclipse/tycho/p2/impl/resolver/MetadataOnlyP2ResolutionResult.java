/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

public class MetadataOnlyP2ResolutionResult implements P2ResolutionResult {

    /**
     * Map of resolution result entries keyed by (type,id,version) tuple
     */
    private final Map<List<String>, Entry> entries = new HashMap<List<String>, Entry>();

    /**
     * @param type
     *            is one of P2Resolver.TYPE_* constants
     * @param id
     *            is Eclipse/OSGi artifact id
     * @param version
     *            is Eclipse/OSGi artifact version
     */
    public void addArtifact(String type, String id, String version, Object installableUnit) {
        // (type,id,version) is unique and not null

        List<String> key = newKey(type, id, version);

        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);

        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(type, id, version, null, null);
            entries.put(key, entry);
        } else {
            throw new IllegalArgumentException("Conflicting results for artifact with (type,id,version)=" + key);
        }

        entry.addInstallableUnit(installableUnit);
    }

    private List<String> newKey(String type, String id, String version) {
        ArrayList<String> key = new ArrayList<String>();
        key.add(type);
        key.add(id);
        key.add(version);
        return key;
    }

    public Collection<Entry> getArtifacts() {
        return entries.values();
    }

    public Set<?> getNonReactorUnits() {
        return Collections.emptySet();
    }
}
