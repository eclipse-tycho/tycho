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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Provider for artifact content in different formats.
 * 
 * <p>
 * Over {@link IArtifactProvider}, this interface adds methods for obtaining artifacts in raw
 * formats, e.g. compressed with the JAR-optimized pack200 format. (With the
 * <tt>IArtifactProvider</tt> interface, artifacts can only be obtained in the canonical format,
 * i.e. the format in which the artifact can be used directly without additional decompression.)
 * </p>
 */
public interface IRawArtifactProvider extends IArtifactProvider {

    /**
     * Return the raw artifact formats in which the given artifact can be provided
     * 
     * @param key
     *            the artifact key to lookup
     * @return the descriptors associated with the given key
     */
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

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
     * Writes the requested artifact in the specified raw format to the given
     * {@link IRawArtifactSink}. In case an error is detected while streaming the artifact (e.g. an
     * MD5 checksum error) and there are other sources available (e.g. in a composite provide), the
     * implementation shall re-attempt the read from these other sources.
     * 
     * @param sink
     *            a sink for a specific artifact in a specific format
     * @param monitor
     *            a progress monitor, or <code>null</code>
     * @throws ArtifactSinkException
     *             if that exception is thrown by the given {@link IArtifactSink}
     * 
     * @see IRawArtifactSink#getArtifactToBeWritten()
     * @see IRawArtifactSink#getArtifactFormatToBeWritten()
     */
    // TODO assert status <-> committed relationship
    // TODO implement&document returned warnings
    public IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException;

}
