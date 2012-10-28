/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import java.io.File;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Provider for artifact files in different formats.
 * 
 * <p>
 * Over {@link IArtifactFileProvider}, this interface adds methods for obtaining artifacts in raw
 * formats, e.g. compressed with the JAR-optimized pack200 format. (With the
 * <tt>IArtifactFileProvider</tt> interface, artifacts can only be obtained in the canonical format,
 * i.e. the format in which the artifact can be used directly without additional decompression.)
 * </p>
 * <p>
 * Over {@link IRawArtifactProvider}, this interface adds a method for obtaining the artifacts as
 * files in the local file system. (With the <tt>IRawArtifactProvider</tt> interface, artifacts can
 * only be obtained as stream.)
 * </p>
 */
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
