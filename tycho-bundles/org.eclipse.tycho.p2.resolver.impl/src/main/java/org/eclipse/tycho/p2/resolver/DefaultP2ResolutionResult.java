/*******************************************************************************
 * Copyright (c) 2011, 2014 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.target.P2TargetPlatform;

public class DefaultP2ResolutionResult implements P2ResolutionResult {

    private final Map<ClassifiedArtifactKey, Entry> entries = new LinkedHashMap<>();
    private final Map<ClassifiedLocation, Entry> entriesByLocation = new LinkedHashMap<>();

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    private final Set<Object/* IInstallableUnit */> nonReactorUnits = new LinkedHashSet<>();

    @Override
    public Collection<Entry> getArtifacts() {
        return entries.values();
    }

    public void addArtifact(ArtifactKey artifactKey, String classifier, IInstallableUnit installableUnit,
            IArtifactKey p2ArtifactKey, P2TargetPlatform resolutionContext) {
        if (resolutionContext.isFileAlreadyAvailable(artifactKey)) {
            addResolvedArtifact(Optional.of(artifactKey), classifier, installableUnit,
                    resolutionContext.getArtifactLocation(artifactKey));
            return;
        }
        ClassifiedArtifactKey key = new ClassifiedArtifactKey(artifactKey, classifier);
        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);
        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                    artifactKey.getVersion(), classifier, () -> resolutionContext.getLocalArtifactFile(p2ArtifactKey));
            entries.put(key, entry);
            File location = entry.getLocation(false);
            if (location != null) {
                entriesByLocation.put(new ClassifiedLocation(location, classifier), entry);
            }
        }
        entry.addInstallableUnit(installableUnit);
    }

    public void addResolvedArtifact(Optional<ArtifactKey> artifactKey, String classifier,
            IInstallableUnit installableUnit, File location) {
        ClassifiedLocation classifiedLocation = new ClassifiedLocation(location, classifier);
        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entriesByLocation
                .get(classifiedLocation);
        Optional<ClassifiedArtifactKey> classifiedArtifactKey = artifactKey
                .map(a -> new ClassifiedArtifactKey(a, classifier));

        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(artifactKey.map(ArtifactKey::getType).orElse(null),
                    artifactKey.map(ArtifactKey::getId).orElse(null),
                    artifactKey.map(ArtifactKey::getVersion).orElse(null), classifier, location);
            entriesByLocation.put(new ClassifiedLocation(location, classifier), entry);
            final DefaultP2ResolutionResultEntry newEntry = entry;
            classifiedArtifactKey.ifPresent(key -> entries.put(key, newEntry));
        } else {
            // bug 375715: entry may have been created for extra IUs from a p2.inf
            if (artifactKey.map(ArtifactKey::getType).isPresent()) {
                if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(entry.getType())
                        && ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(artifactKey.get().getType())) {
                    /*
                     * TODO 348586 For eclipse-repository projects containing products, we currently
                     * create an eclipse-product entry using id and version of one of the products
                     * at random. This seems wrong - with eclipse-product there should be only one
                     * product per project, or additional product should be required to specify a
                     * classifier.
                     */
                    // skip overwrite check

                } else if (entry.getType() != null && classifier == null) {
                    if (!artifactKey.get().getType().equals(entry.getType()) // 
                            || !artifactKey.get().getId().equals(entry.getId()) //
                            || !artifactKey.get().getVersion().equals(entry.getVersion())) {
                        // bug 430728: prevent that p2.inf "artifacts" overwrite the type of the main artifact
                        throw new RuntimeException("Ambiguous main artifact of the project for "
                                + artifactKey.get().getId()
                                + ". Make sure that additional units added via p2.inf specify a 'maven-classifier' property.");
                    }
                }

                // type, id, and version for the artifact/project location is only known now -> update in entry
                entry.setType(artifactKey.get().getType());
                entry.setId(artifactKey.get().getId());
                entry.setVersion(artifactKey.get().getVersion());
            }
        }

        entry.addInstallableUnit(installableUnit);
    }

    public void removeEntriesWithUnknownType() {
        entries.values().removeIf(entry -> entry.getType() == null);
    }

    public void addNonReactorUnit(Object/* IInstallableUnit */ installableUnit) {
        this.nonReactorUnits.add(installableUnit);
    }

    public void addNonReactorUnits(Set<?/* IInstallableUnit */> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
    }

    @Override
    public Set<?> getNonReactorUnits() {
        return nonReactorUnits;
    }
}
