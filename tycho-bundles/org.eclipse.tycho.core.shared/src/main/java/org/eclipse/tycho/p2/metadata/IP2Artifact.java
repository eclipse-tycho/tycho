/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Encapsulates an artifact, i.e. a File, and associated p2 metadata.
 * 
 * @TODO reconcile with IDependencyMetadata, which serves essentially the same purpose
 */
public interface IP2Artifact {
    public File getLocation();

    public Set<IInstallableUnit> getInstallableUnits();

    public IArtifactDescriptor getArtifactDescriptor();
}
