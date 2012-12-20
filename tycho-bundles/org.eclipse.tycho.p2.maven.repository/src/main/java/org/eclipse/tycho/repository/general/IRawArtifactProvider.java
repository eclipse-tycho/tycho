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

import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public interface IRawArtifactProvider extends IArtifactProvider {

    /**
     * Returns <code>true</code> if this a provider for an artifact in the given format
     * 
     * @param descriptor
     *            an artifact descriptor
     * @return <code>true</code> if this instance can provide the artifact as raw artifact in the
     *         described format
     */
    public boolean contains(IArtifactDescriptor descriptor);

    /**
     * Writes the artifact in the described raw format to the given output stream
     * 
     * @param descriptor
     *            the key and format of the artifact to transfer
     * @param destination
     *            the stream to write the raw artifact to
     * @param monitor
     *            a progress monitor, or <code>null</code>
     * @return the result of the artifact transfer
     */
    public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

    /**
     * Return the raw artifact formats in which the given artifact can be provided
     * 
     * @param key
     *            the artifact key to lookup
     * @return the descriptors associated with the given key
     */
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

}
