/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;

/**
 * Provider for artifact content.
 */
public interface IArtifactProvider extends IQueryable<IArtifactKey> {

    /**
     * Returns <code>true</code> if this is a provider for the given artifact.
     * 
     * @param key
     *            An artifact key
     * @return <code>true</code> if this instance can provide the artifact for the given key
     */
    public boolean contains(IArtifactKey key);

    /**
     * Writes the requested artifact to the given {@link IArtifactSink}.
     * 
     * <p>
     * The implementation is free to pick the most suitable internal storage format to serve the
     * request. If an error is detected while streaming the artifact (e.g. an MD5 checksum error),
     * the implementation may re-attempt the read from all other available sources. In case there
     * were multiple read attempts, a multi-status with the results of all attempts is returned.
     * </p>
     * 
     * @param sink
     *            A sink for a specific artifact. When this method returns, the sink will either be
     *            closed (with {@link IArtifactSink#commitWrite()} or
     *            {@link IArtifactSink#abortWrite()}, depending on the status), or not have received
     *            any content.
     * @param monitor
     *            A progress monitor, or <code>null</code>
     * @return A non-fatal status (warning or better) if the read operation was successful.
     * @throws ArtifactSinkException
     *             if that exception is thrown by the given {@link IArtifactSink}
     * 
     * @see IArtifactSink#getArtifactToBeWritten()
     */
    public IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException;

}
