/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import java.io.File;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * Provider for artifact files.
 * 
 * <p>
 * Over {@link IArtifactProvider}, this interface adds a method for obtaining the artifacts as files
 * in the local file system. (With the <tt>IArtifactProvider</tt> interface, artifacts can only be
 * obtained as stream.)
 * </p>
 */
public interface IArtifactFileProvider extends IArtifactProvider {

    /**
     * Returns the file system location of the given artifact.
     * 
     * @param key
     *            An artifact key
     * @return The location of the specified artifact, or <code>null</code> the given artifact does
     *         not exist.
     */
    public File getArtifactFile(IArtifactKey key);

}
