/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Issue #626 - Classpath computation must take fragments into account 
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.target.ArtifactTypeHelper;
import org.eclipse.tycho.p2.target.P2TargetPlatform;

public class DefaultP2ResolutionResult implements P2ResolutionResult {

    private final Map<ClassifiedArtifactKey, Entry> entries = new LinkedHashMap<>();
    private final Map<ClassifiedLocation, Entry> entriesByLocation = new LinkedHashMap<>();

    /**
     * Set of installable unit in the target platform of the module that do not come from the local
     * reactor.
     */
    private final Set<IInstallableUnit> nonReactorUnits = new LinkedHashSet<>();
    private P2TargetPlatform resolutionContext;
    private List<Entry> fragments;

    public DefaultP2ResolutionResult(Collection<IInstallableUnit> dependencyFragments,
            P2TargetPlatform resolutionContext) {
        this.resolutionContext = resolutionContext;
        fragments = dependencyFragments.stream().map(iu -> {
            DefaultArtifactKey artifactKey = new DefaultArtifactKey(ArtifactType.TYPE_BUNDLE_FRAGMENT, iu.getId(),
                    iu.getVersion().toString());
            final DefaultP2ResolutionResultEntry entry;
            if (resolutionContext.isFileAlreadyAvailable(artifactKey)) {
                entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                        artifactKey.getVersion(), null, resolutionContext.getArtifactLocation(artifactKey));
            } else {
                entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                        artifactKey.getVersion(), null, () -> {
                            File res = resolutionContext
                                    .getLocalArtifactFile(ArtifactTypeHelper.toP2ArtifactKey(artifactKey));
                            resolutionContext.saveLocalMavenRepository(); // store just downloaded artifacts in local Maven repo index
                            return res;
                        });
            }
            entry.addInstallableUnit(iu);
            return entry;
        }).collect(Collectors.toList());
    }

    @Override
    public Collection<Entry> getArtifacts() {
        return entries.values();
    }

    public void addArtifact(ArtifactKey artifactKey, String classifier, IInstallableUnit installableUnit,
            IArtifactKey p2ArtifactKey) {
        if (resolutionContext.isFileAlreadyAvailable(artifactKey)) {
            addResolvedArtifact(Optional.of(artifactKey), classifier, installableUnit,
                    resolutionContext.getArtifactLocation(artifactKey));
            return;
        }
        ClassifiedArtifactKey key = new ClassifiedArtifactKey(artifactKey, classifier);
        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);
        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                    artifactKey.getVersion(), classifier, () -> {
                        File res = resolutionContext.getLocalArtifactFile(p2ArtifactKey);
                        resolutionContext.saveLocalMavenRepository(); // store just downloaded artifacts in local Maven repo index
                        return res;
                    });
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
        final DefaultP2ResolutionResultEntry existingEntryForLocation = (DefaultP2ResolutionResultEntry) entriesByLocation
                .get(classifiedLocation);
        Optional<ClassifiedArtifactKey> classifiedArtifactKey = artifactKey
                .map(a -> new ClassifiedArtifactKey(a, classifier));

        if (existingEntryForLocation == null) {
            final DefaultP2ResolutionResultEntry newEntry = new DefaultP2ResolutionResultEntry(
                    artifactKey.map(ArtifactKey::getType).orElse(null),
                    artifactKey.map(ArtifactKey::getId).orElse(null),
                    artifactKey.map(ArtifactKey::getVersion).orElse(null), classifier, location);
            newEntry.addInstallableUnit(installableUnit);
            entriesByLocation.put(new ClassifiedLocation(location, classifier), newEntry);
            classifiedArtifactKey.ifPresent(key -> entries.put(key, newEntry));
        } else {
            existingEntryForLocation.addInstallableUnit(installableUnit);
            artifactKey.ifPresent(key -> {
                if (key.getType() == null) {
                    return; // no extra info to set
                }
                // bug 375715: entry may have been created for extra IUs from a p2.inf
                if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(existingEntryForLocation.getType())
                        && ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(key.getType())) {
                    /*
                     * TODO 348586 For eclipse-repository projects containing products, we currently
                     * create an eclipse-product entry using id and version of one of the products
                     * at random. This seems wrong - with eclipse-product there should be only one
                     * product per project, or additional product should be required to specify a
                     * classifier.
                     */
                    // skip overwrite check
                } else if (existingEntryForLocation.getType() != null && classifier == null) {
                    if (!key.getType().equals(existingEntryForLocation.getType()) // 
                            || !key.getId().equals(existingEntryForLocation.getId()) //
                            || !key.getVersion().equals(existingEntryForLocation.getVersion())) {
                        // bug 430728: prevent that p2.inf "artifacts" overwrite the type of the main artifact
                        throw new RuntimeException("Ambiguous main artifact of the project for "
                                + artifactKey.get().getId()
                                + ". Make sure that additional units added via p2.inf specify a 'maven-classifier' property.");
                    }
                }

                // type, id, and version for the artifact/project location is only known now -> update in entry
                existingEntryForLocation.setType(key.getType());
                existingEntryForLocation.setId(key.getId());
                existingEntryForLocation.setVersion(key.getVersion());
                classifiedArtifactKey.ifPresent(classifiedKey -> {
                    if (!entries.containsKey(classifiedKey)) {
                        entries.put(classifiedKey, existingEntryForLocation);
                    }
                });
            });
        }
    }

    public void removeEntriesWithUnknownType() {
        entries.values().removeIf(entry -> entry.getType() == null);
    }

    public void addNonReactorUnit(IInstallableUnit installableUnit) {
        this.nonReactorUnits.add(installableUnit);
    }

    public void addNonReactorUnits(Set<IInstallableUnit> installableUnits) {
        this.nonReactorUnits.addAll(installableUnits);
    }

    @Override
    public Set<IInstallableUnit> getNonReactorUnits() {
        return nonReactorUnits;
    }

    @Override
    public Collection<Entry> getDependencyFragments() {
        return fragments;
    }
}
