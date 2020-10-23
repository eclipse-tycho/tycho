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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.target.P2TargetPlatform;

public class DefaultP2ResolutionResult implements P2ResolutionResult {

    private final Map<ClassifiedArtifactKey, Entry> entries = new LinkedHashMap<>();

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
        ClassifiedArtifactKey key = new ClassifiedArtifactKey(artifactKey, classifier);

        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);

        if (entry == null) {
            if (resolutionContext.isFileAlreadyAvailable(artifactKey)) {
                entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                        artifactKey.getVersion(), classifier, resolutionContext.getLocalArtifactFile(p2ArtifactKey));
            } else {
                entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                        artifactKey.getVersion(), classifier,
                        () -> resolutionContext.getLocalArtifactFile(p2ArtifactKey));
            }
            entries.put(key, entry);
        } else {
            // bug 375715: entry may have been created for extra IUs from a p2.inf
            if (artifactKey.getType() != null) {
                if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(entry.getType())
                        && ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(artifactKey.getType())) {
                    /*
                     * TODO 348586 For eclipse-repository projects containing products, we currently
                     * create an eclipse-product entry using id and version of one of the products
                     * at random. This seems wrong - with eclipse-product there should be only one
                     * product per project, or additional product should be required to specify a
                     * classifier.
                     */
                    // skip overwrite check

                } else if (entry.getType() != null && classifier == null) {
                    if (!artifactKey.getType().equals(entry.getType()) || !artifactKey.getId().equals(entry.getId())
                            || !artifactKey.getVersion().equals(entry.getVersion())) {
                        // bug 430728: prevent that p2.inf "artifacts" overwrite the type of the main artifact
                        throw new RuntimeException("Ambiguous main artifact of the project for " + artifactKey.getId()
                                + ". Make sure that additional units added via p2.inf specify a 'maven-classifier' property.");
                    }
                }

                // type, id, and version for the artifact/project location is only known now -> update in entry
                entry.setType(artifactKey.getType());
                entry.setId(artifactKey.getId());
                entry.setVersion(artifactKey.getVersion());
            }
        }

        entry.addInstallableUnit(installableUnit);
    }

    public void addResolvedArtifact(ArtifactKey artifactKey, String classifier, IInstallableUnit installableUnit,
            File location) {
        ClassifiedArtifactKey key = new ClassifiedArtifactKey(artifactKey, classifier);

        DefaultP2ResolutionResultEntry entry = (DefaultP2ResolutionResultEntry) entries.get(key);

        if (entry == null) {
            entry = new DefaultP2ResolutionResultEntry(artifactKey.getType(), artifactKey.getId(),
                    artifactKey.getVersion(), classifier, location);
            entries.put(key, entry);
        } else {
            // bug 375715: entry may have been created for extra IUs from a p2.inf
            if (artifactKey.getType() != null) {
                if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(entry.getType())
                        && ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(artifactKey.getType())) {
                    /*
                     * TODO 348586 For eclipse-repository projects containing products, we currently
                     * create an eclipse-product entry using id and version of one of the products
                     * at random. This seems wrong - with eclipse-product there should be only one
                     * product per project, or additional product should be required to specify a
                     * classifier.
                     */
                    // skip overwrite check

                } else if (entry.getType() != null && classifier == null) {
                    if (!artifactKey.getType().equals(entry.getType()) || !artifactKey.getId().equals(entry.getId())
                            || !artifactKey.getVersion().equals(entry.getVersion())) {
                        // bug 430728: prevent that p2.inf "artifacts" overwrite the type of the main artifact
                        throw new RuntimeException("Ambiguous main artifact of the project for " + artifactKey.getId()
                                + ". Make sure that additional units added via p2.inf specify a 'maven-classifier' property.");
                    }
                }

                // type, id, and version for the artifact/project location is only known now -> update in entry
                entry.setType(artifactKey.getType());
                entry.setId(artifactKey.getId());
                entry.setVersion(artifactKey.getVersion());
            }
        }

        entry.addInstallableUnit(installableUnit);
    }

    public void removeEntriesWithUnknownType() {
        for (Iterator<java.util.Map.Entry<ClassifiedArtifactKey, Entry>> iterator = entries.entrySet()
                .iterator(); iterator.hasNext();) {
            if (iterator.next().getValue().getType() == null) {
                iterator.remove();
            }
        }
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

    protected static <T> boolean eq(T a, T b) {
        return a != null ? a.equals(b) : b == null;
    }

}
