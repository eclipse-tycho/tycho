/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class UnmodifiableDependencyMetadata implements IDependencyMetadata {

    private Set<IInstallableUnit> units;
    private DependencyMetadataType dependencyMetadataType;

    public UnmodifiableDependencyMetadata(Set<IInstallableUnit> units, DependencyMetadataType type) {
        this.dependencyMetadataType = type;
        this.units = Collections.unmodifiableSet(units);
    }

    @Override
    public Set<IInstallableUnit> getDependencyMetadata(DependencyMetadataType type) {
        if (dependencyMetadataType == type) {
            return getDependencyMetadata();
        }
        return Set.of();
    }

    @Override
    public Set<IInstallableUnit> getDependencyMetadata() {
        return units;
    }

    @Override
    public void setDependencyMetadata(DependencyMetadataType type, Collection<IInstallableUnit> units) {
        throw new UnsupportedOperationException();
    }

}
