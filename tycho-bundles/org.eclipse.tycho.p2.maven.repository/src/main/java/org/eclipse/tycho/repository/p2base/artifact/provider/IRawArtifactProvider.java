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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

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
     * Return the raw artifact formats in which the given artifact can be provided.
     * 
     * @param key
     *            An artifact key
     * @return The descriptors associated with the given key
     */
    public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

    /**
     * Returns <code>true</code> if this a provider for an artifact in the given format
     * 
     * @param descriptor
     *            An artifact descriptor
     * @return <code>true</code> if this instance can provide the artifact as raw artifact in the
     *         described format
     */
    public boolean contains(IArtifactDescriptor descriptor);

    /**
     * Writes the requested artifact in the specified raw format to the given
     * {@link IRawArtifactSink}.
     * 
     * <p>
     * If an error is detected while streaming the artifact (e.g. an MD5 checksum error) and there
     * are other sources available (e.g. in a composite provider), the implementation may re-attempt
     * the read from these other sources. In case there were multiple read attempts, a multi-status
     * with the results of all attempts is returned.
     * </p>
     * 
     * @param sink
     *            A sink for a specific artifact in a specific format. When this method returns, the
     *            sink will either be closed (with {@link IRawArtifactSink#commitWrite()} or
     *            {@link IRawArtifactSink#abortWrite()}, depending on the status), or not have
     *            received any content.
     * @param monitor
     *            A progress monitor, or <code>null</code>
     * @return A non-fatal status (warning or better) if the read operation was successful.
     * @throws ArtifactSinkException
     *             if that exception is thrown by the given {@link IArtifactSink}
     * 
     * @see IRawArtifactSink#getArtifactToBeWritten()
     * @see IRawArtifactSink#getArtifactFormatToBeWritten()
     */
    public IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException;

}
