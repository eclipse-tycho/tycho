/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;

public class DependencyMetadata implements IDependencyMetadata {

    private Set<Object> metadata;
    private Set<Object> secondaryMetadata;
    private Set<IArtifactDescriptor> artifacts;

    public Set<Object /* IInstallableUnit */> getMetadata(boolean primary) {
        return primary ? metadata : secondaryMetadata;
    }

    public Set<Object /* IInstallableUnit */> getMetadata() {
        LinkedHashSet<Object> result = new LinkedHashSet<Object>();
        result.addAll(metadata);
        result.addAll(secondaryMetadata);
        return result;
    }

    public void setMetadata(boolean primary, Collection<IInstallableUnit> units) {
        if (primary) {
            metadata = new LinkedHashSet<Object>(units);
        } else {
            secondaryMetadata = new LinkedHashSet<Object>(units);
        }
    }

    public void setArtifacts(Collection<IArtifactDescriptor> artifacts) {
        this.artifacts = new LinkedHashSet<IArtifactDescriptor>(artifacts);
    }

    public Set<IArtifactDescriptor> getArtifactDescriptors() {
        return artifacts;
    }

    public Set<IInstallableUnit> getInstallableUnits() {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();
        for (Object unit : metadata) {
            result.add((IInstallableUnit) unit);
        }
        for (Object unit : secondaryMetadata) {
            result.add((IInstallableUnit) unit);
        }
        return result;
    }
}
