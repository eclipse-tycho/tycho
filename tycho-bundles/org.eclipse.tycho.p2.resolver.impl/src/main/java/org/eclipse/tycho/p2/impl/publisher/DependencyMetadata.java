/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.impl.publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.DependencyMetadataScope;
import org.eclipse.tycho.IDependencyMetadata;

public class DependencyMetadata implements IDependencyMetadata {

    private Map<DependencyMetadataScope, Set<Object>> typeMap = new TreeMap<>();
    private Set<IArtifactDescriptor> artifacts;

    @Override
    public Set<Object /* IInstallableUnit */> getDependencyMetadata(DependencyMetadataScope type) {
        return typeMap.getOrDefault(type, Collections.emptySet());
    }

    @Override
    public Set<Object /* IInstallableUnit */> getDependencyMetadata() {
        LinkedHashSet<Object> result = new LinkedHashSet<>();
        result.addAll(getDependencyMetadata(DependencyMetadataScope.SEED));
        result.addAll(getDependencyMetadata(DependencyMetadataScope.RESOLVE));
        return result;
    }

    public void setDependencyMetadata(DependencyMetadataScope type, Collection<?> units) {

        typeMap.put(type, new LinkedHashSet<Object>(units));
    }

    public void setArtifacts(Collection<IArtifactDescriptor> artifacts) {
        this.artifacts = new LinkedHashSet<>(artifacts);
    }

    public Set<IArtifactDescriptor> getArtifactDescriptors() {
        return artifacts;
    }

    public Set<IInstallableUnit> getInstallableUnits() {
        return typeMap.values().stream().flatMap(Collection::stream).filter(IInstallableUnit.class::isInstance)
                .map(IInstallableUnit.class::cast).distinct().collect(Collectors.toSet());
    }
}
