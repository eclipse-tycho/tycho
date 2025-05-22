/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.container.Module;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;

public class ModuleArtifactDescriptor implements ArtifactDescriptor {

    private Module module;

    public ModuleArtifactDescriptor(Module module) {
        this.module = module;
    }

    @Override
    public ArtifactKey getKey() {
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, module.getCurrentRevision().getSymbolicName(),
                module.getCurrentRevision().getVersion().toString());
    }

    @Override
    public Optional<File> getLocation() {
        Object revisionInfo = module.getCurrentRevision().getRevisionInfo();
        if (revisionInfo instanceof File f) {
            return Optional.of(f);
        }
        return Optional.of(new File(module.getBundle().getLocation()));
    }

    @Override
    public CompletableFuture<File> fetchArtifact() {
        return CompletableFuture.completedFuture(getLocation().get());
    }

    @Override
    public ReactorProject getMavenProject() {
        return null;
    }

    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits() {
        return List.of();
    }

}
