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

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

public class DefaultP2ResolutionResult implements P2ResolutionResult {

    private final Map<ClassifiedLocation, Entry> entries = new LinkedHashMap<ClassifiedLocation, P2ResolutionResult.Entry>();

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    private final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<Object>();

    public Collection<Entry> getArtifacts() {
        return entries.values();
    }

    public void addArtifact(String type, String id, String version, boolean primary, File location, String classifier,
            IInstallableUnit installableUnit) {
        // (location,classifier) is not null, but can have multiple associated IUs

        ClassifiedLocation key = new ClassifiedLocation(location, classifier);

        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);

        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(type, id, version, location, classifier);
            entries.put(key, entry);
        } else {
            // bug 375715: entry may have been created for extra IUs from a p2.inf
            if (primary) {
                // set correct id/version for this entry
                entry.setId(id);
                entry.setVersion(version);
            }

        }

        entry.addInstallableUnit(installableUnit);
    }

    public void addNonReactorUnit(Object/* IInstallableUnit */installableUnit) {
        this.nonReactorUnits.add(installableUnit);
    }

    public void addNonReactorUnits(Set<?/* IInstallableUnit */> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
    }

    public Set<?> getNonReactorUnits() {
        return nonReactorUnits;
    }

    protected static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

}
