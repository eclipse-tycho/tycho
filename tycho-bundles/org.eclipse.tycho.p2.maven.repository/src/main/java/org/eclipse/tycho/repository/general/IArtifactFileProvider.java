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

import org.eclipse.equinox.p2.metadata.IArtifactKey;

public interface IArtifactFileProvider extends IArtifactProvider {

    /**
     * Returns the local file system location of the given artifact.
     * 
     * @param key
     *            an artifact key
     * @return the location of the specified artifact, or <code>null</code> the given artifact does
     *         not exist.
     */
    public File getArtifactFile(IArtifactKey key);

}
