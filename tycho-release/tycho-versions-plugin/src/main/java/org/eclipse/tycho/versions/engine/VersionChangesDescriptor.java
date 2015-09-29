/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class holds the set of changes that are applied during a {@link VersionsEngine} execution as
 * well as some configuration that need to be passed to {@link MetadataManipulator}s.
 */
public class VersionChangesDescriptor {

    private final Set<VersionChange> aritfactsVersionChanges;
    private final Set<PackageVersionChange> packageVersionChanges;

    private final VersionRangeUpdateStrategy versionRangeUpdateStrategy;

    public VersionChangesDescriptor(Set<VersionChange> originalVersionChanges,
            VersionRangeUpdateStrategy versionRangeUpdateStrategy) {
        this.aritfactsVersionChanges = new HashSet<>(originalVersionChanges);
        this.versionRangeUpdateStrategy = versionRangeUpdateStrategy;
        this.packageVersionChanges = new HashSet<>();
    }

    public Set<VersionChange> getVersionChanges() {
        // Creates a copy to avoid concurrent modification exception if used during addMoreChanges phase
        return Collections.unmodifiableSet(new HashSet<>(aritfactsVersionChanges));
    }

    public boolean addVersionChange(VersionChange versionChange) {
        return aritfactsVersionChanges.add(versionChange);
    }

    public VersionChange findVersionChangeByArtifactId(String symbolicName) {
        for (VersionChange versionChange : aritfactsVersionChanges) {
            if (versionChange.getArtifactId().equals(symbolicName)) {
                return versionChange;
            }
        }
        return null;
    }

    public Set<PackageVersionChange> getPackageVersionChanges() {
        return Collections.unmodifiableSet(packageVersionChanges);
    }

    public VersionRangeUpdateStrategy getVersionRangeUpdateStrategy() {
        return versionRangeUpdateStrategy;
    }

    public boolean addPackageVersionChanges(Set<PackageVersionChange> changes) {
        return packageVersionChanges.addAll(changes);
    }

    public PackageVersionChange findPackageVersionChange(String packageName) {
        for (PackageVersionChange versionChange : packageVersionChanges) {
            if (versionChange.getPackageName().equals(packageName)) {
                return versionChange;
            }
        }
        return null;
    }

}
