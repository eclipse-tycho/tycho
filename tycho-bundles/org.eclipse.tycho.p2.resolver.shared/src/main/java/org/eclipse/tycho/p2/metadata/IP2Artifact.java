/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

import java.io.File;
import java.util.Set;

/**
 * Encapsulates an artifact, i.e. a File, and associated p2 metadata.
 * 
 * @TODO reconcile with IDependencyMetadata, which serves essentially the same purpose
 */
public interface IP2Artifact {
    public File getLocation();

    public Set<Object /* IInstallableUnit */> getInstallableUnits();

    public Object/* IArtifactDescriptor */getArtifactDescriptor();
}
