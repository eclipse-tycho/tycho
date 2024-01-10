/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This class holds the set of changes that are applied during a {@link VersionsEngine} execution as
 * well as some configuration that need to be passed to {@link MetadataManipulator}s.
 */
public class VersionChangesDescriptor {

    private final Set<PomVersionChange> aritfactsVersionChanges;
    private final Set<PackageVersionChange> packageVersionChanges;

    private final VersionRangeUpdateStrategy versionRangeUpdateStrategy;
    private Collection<ProjectMetadata> projects;

    public VersionChangesDescriptor(Set<PomVersionChange> originalVersionChanges,
            VersionRangeUpdateStrategy versionRangeUpdateStrategy, Collection<ProjectMetadata> projects) {
        this.projects = projects;
        this.aritfactsVersionChanges = new LinkedHashSet<>(originalVersionChanges);
        this.versionRangeUpdateStrategy = versionRangeUpdateStrategy;
        this.packageVersionChanges = new LinkedHashSet<>();
    }

    public Optional<ProjectMetadata> findMetadataByBasedir(File baseDir) {
        Path path = baseDir.toPath();
        for (ProjectMetadata meta : projects) {
            Path projectPath = meta.getBasedir().toPath();
            try {
                if (Files.isSameFile(projectPath, path)) {
                    return Optional.of(meta);
                }
            } catch (IOException e) {
            }
        }
        return Optional.empty();

    }

    public Set<PomVersionChange> getVersionChanges() {
        // Creates a copy to avoid concurrent modification exception if used during addMoreChanges phase
        return Collections.unmodifiableSet(new HashSet<>(aritfactsVersionChanges));
    }

    public boolean addVersionChange(PomVersionChange versionChange) {
        return aritfactsVersionChanges.add(versionChange);
    }

    public VersionChange findVersionChangeByArtifactId(String symbolicName) {
        for (PomVersionChange versionChange : aritfactsVersionChanges) {
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
