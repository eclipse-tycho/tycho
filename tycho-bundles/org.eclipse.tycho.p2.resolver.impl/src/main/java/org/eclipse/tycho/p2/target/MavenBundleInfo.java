/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2.target;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;

public class MavenBundleInfo {

    private IInstallableUnit unit;
    private IArtifactDescriptor descriptor;
    private IArtifactFacade artifact;

    public MavenBundleInfo(IInstallableUnit unit, IArtifactDescriptor descriptor, IArtifactFacade artifact) {
        this.unit = unit;
        this.descriptor = descriptor;
        this.artifact = artifact;
    }

    public IInstallableUnit getUnit() {
        return unit;
    }

    public IArtifactDescriptor getDescriptor() {
        return descriptor;
    }

    public IArtifactFacade getArtifact() {
        return artifact;
    }

}
