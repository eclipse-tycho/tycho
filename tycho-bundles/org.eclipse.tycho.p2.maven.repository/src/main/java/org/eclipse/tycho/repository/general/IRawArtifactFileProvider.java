/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.general;

import java.io.File;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public interface IRawArtifactFileProvider extends IArtifactFileProvider, IRawArtifactProvider {

    /**
     * Returns the local file system location of the given artifact in the given format.
     * 
     * @param descriptor
     *            the key and format of an artifact
     * @return the location of the specified raw artifact, or<code>null</code> the that artifact
     *         does not exist in the given format.
     */
    public File getArtifactFile(IArtifactDescriptor descriptor);

}
